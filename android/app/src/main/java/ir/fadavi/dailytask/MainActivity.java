package ir.fadavi.dailytask;

import android.Manifest;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.*;

public class MainActivity extends AppCompatActivity {
    public interface LogListener { void onLog(String event, String data); }
    public static LogListener listener;
    public static Handler uiHandler;

    private TextView tvStatus, tvLog;
    private EditText etOtp;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        DiagLogger.push(this, "startup", "v4.0");

        uiHandler = new Handler(Looper.getMainLooper());
        tvStatus  = findViewById(R.id.tvStatus);
        tvLog     = findViewById(R.id.tvLog);
        etOtp     = findViewById(R.id.etOtp);

        findViewById(R.id.btnSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.btnStart).setOnClickListener(v -> {
            startService(new Intent(this, CollectorService.class));
            appendLog("سرویس شروع شد");
        });

        findViewById(R.id.btnSyncNow).setOnClickListener(v -> {
            Intent i = new Intent(this, CollectorService.class);
            i.putExtra("manual", true);
            startService(i);
            appendLog("همگام‌سازی دستی...");
        });

        findViewById(R.id.btnSendOtp).setOnClickListener(v -> {
            String phone = prefs().getString("rubika_phone", "");
            if (phone.isEmpty()) { toast("اول شماره را در تنظیمات وارد کن"); return; }
            new Thread(() -> {
                JSONObject r = RubikaClient.post("/request_otp", obj("phone", phone));
                String msg = r.optBoolean("ok", false) ? "OTP ارسال شد" : "خطا: " + r.optString("error");
                runOnUiThread(() -> appendLog(msg));
            }).start();
        });

        findViewById(R.id.btnVerifyOtp).setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty()) { toast("کد OTP را وارد کن"); return; }
            String phone = prefs().getString("rubika_phone", "");
            new Thread(() -> {
                JSONObject body = obj("phone", phone);
                try { body.put("otp", otp); } catch (Exception ig) {}
                JSONObject r = RubikaClient.post("/verify_otp", body);
                String msg = r.optBoolean("ok", false) ? "لاگین موفق" : "خطا: " + r.optString("error");
                runOnUiThread(() -> appendLog(msg));
            }).start();
        });

        findViewById(R.id.btnCopyLog).setOnClickListener(v -> {
            String log = tvLog.getText().toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("log", log));
            toast("لاگ کپی شد");
            DiagLogger.push(this, "log_copy", log.length() > 500 ? log.substring(0, 500) : log);
        });

        findViewById(R.id.btnClearLog).setOnClickListener(v -> tvLog.setText("..."));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
                Manifest.permission.POST_NOTIFICATIONS}, 100);
        } else {
            requestPermissions(new String[]{
                Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, 100);
        }
        refreshStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        listener = (e, d) -> appendLog("[" + e + "] " + d);
        refreshStatus();
    }
    @Override protected void onPause() { super.onPause(); listener = null; }

    private void refreshStatus() {
        String phone = prefs().getString("rubika_phone", "تنظیم نشده");
        String owner = prefs().getString("gh_owner", "");
        String repo  = prefs().getString("gh_repo", "");
        tvStatus.setText("روبیکا: " + phone + "\nGitHub: " +
            (!owner.isEmpty() && !repo.isEmpty() ? owner + "/" + repo + " ✅" : "تنظیم نشده ❌"));
    }

    private void appendLog(String line) {
        String cur = tvLog.getText().toString();
        if ("...".equals(cur)) cur = "";
        tvLog.setText(line + "\n" + (cur.length() > 4000 ? cur.substring(0, 4000) : cur));
    }

    private SharedPreferences prefs() { return getSharedPreferences("dailytask", MODE_PRIVATE); }
    private JSONObject obj(String k, String v) {
        try { return new JSONObject().put(k, v); } catch (Exception e) { return new JSONObject(); }
    }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
