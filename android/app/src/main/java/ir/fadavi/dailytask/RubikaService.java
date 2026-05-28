package ir.fadavi.dailytask;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.chaquo.python.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.concurrent.*;

public class RubikaService extends Service {

    private static final String CHANNEL_ID = "rubika_service";
    private static final int    NOTIF_ID   = 1001;
    private static final long   INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes

    private ScheduledExecutorService scheduler;
    private GitHubLogger ghLogger;

    public static final String ACTION_REQUEST_OTP = "ir.fadavi.dailytask.REQUEST_OTP";
    public static final String ACTION_VERIFY_OTP  = "ir.fadavi.dailytask.VERIFY_OTP";
    public static final String EXTRA_PHONE = "phone";
    public static final String EXTRA_OTP   = "otp";

    @Override
    public void onCreate() {
        super.onCreate();
        ghLogger = new GitHubLogger(this);
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("دستیار روزانه در حال اجرا..."));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_REQUEST_OTP.equals(action)) {
                String phone = intent.getStringExtra(EXTRA_PHONE);
                new Thread(() -> doRequestOtp(phone)).start();
                return START_STICKY;
            }
            if (ACTION_VERIFY_OTP.equals(action)) {
                String phone = intent.getStringExtra(EXTRA_PHONE);
                String otp   = intent.getStringExtra(EXTRA_OTP);
                new Thread(() -> doVerifyOtp(phone, otp)).start();
                return START_STICKY;
            }
        }

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::collectAndLog, 0, INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
        return START_STICKY;
    }

    private void doRequestOtp(String phone) {
        try {
            Python py = Python.getInstance();
            PyObject bridge = py.getModule("rubika_bridge");
            bridge.callAttr("set_session_path", sessionPath(phone));
            PyObject result = bridge.callAttr("request_otp", phone);
            broadcast("OTP_SENT", result.toString());
        } catch (Exception e) {
            broadcast("OTP_ERROR", e.getMessage());
        }
    }

    private void doVerifyOtp(String phone, String otp) {
        try {
            Python py = Python.getInstance();
            PyObject bridge = py.getModule("rubika_bridge");
            bridge.callAttr("set_session_path", sessionPath(phone));
            PyObject result = bridge.callAttr("verify_otp", phone, otp);
            broadcast("OTP_VERIFIED", result.toString());
        } catch (Exception e) {
            broadcast("OTP_ERROR", e.getMessage());
        }
    }

    private void collectAndLog() {
        SharedPreferences prefs = getSharedPreferences("dailytask", MODE_PRIVATE);
        String phone = prefs.getString("rubika_phone", "");
        if (phone.isEmpty()) return;

        try {
            Python py = Python.getInstance();
            PyObject bridge = py.getModule("rubika_bridge");
            bridge.callAttr("set_session_path", sessionPath(phone));

            boolean loggedIn = bridge.callAttr("is_logged_in", phone).toBoolean();
            if (!loggedIn) {
                AppLog.w("RubikaService", "not logged in, skipping collect");
                return;
            }

            // Fetch groups
            PyObject groupsResult = bridge.callAttr("get_groups", phone, 20);
            JSONObject groupsJson = new JSONObject(groupsResult.toString());
            if (!groupsJson.optBoolean("ok", false)) {
                AppLog.e("RubikaService", "get_groups error: " + groupsJson.optString("error"));
                return;
            }

            JSONArray groups = groupsJson.getJSONArray("groups");
            AppLog.i("RubikaService", "found " + groups.length() + " groups");

            // For each group, fetch last messages and push to GitHub
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                String guid  = group.getString("guid");
                String title = group.getString("title");

                PyObject msgsResult = bridge.callAttr("get_group_messages", phone, guid, 10);
                JSONObject msgsJson = new JSONObject(msgsResult.toString());

                if (msgsJson.optBoolean("ok", false)) {
                    JSONObject payload = new JSONObject();
                    payload.put("source", "rubika");
                    payload.put("group_guid", guid);
                    payload.put("group_title", title);
                    payload.put("messages", msgsJson.getJSONArray("messages"));
                    ghLogger.push("rubika_groups", payload);
                }
            }

            updateNotification("آخرین همگام‌سازی: " + currentTime());

        } catch (Exception e) {
            AppLog.e("RubikaService", "collectAndLog: " + e.getMessage());
        }
    }

    private String sessionPath(String phone) {
        File dir = getFilesDir();
        String safe = phone.replaceAll("[^0-9]", "");
        return new File(dir, "rubika_" + safe).getAbsolutePath();
    }

    private String currentTime() {
        return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date());
    }

    private void broadcast(String event, String data) {
        Intent i = new Intent("ir.fadavi.dailytask.RUBIKA_EVENT");
        i.putExtra("event", event);
        i.putExtra("data", data);
        sendBroadcast(i);
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "دستیار روزانه", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Daily Task Manager")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        getSystemService(NotificationManager.class)
            .notify(NOTIF_ID, buildNotification(text));
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) scheduler.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
