package ir.fadavi.dailytask;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvLogs;
    private EditText etOtp;
    private Button btnSendOtp, btnVerifyOtp, btnStartService, btnSettings;

    private BroadcastReceiver rubikaReceiver;
    private String pendingPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Python (Chaquopy) once
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        tvStatus       = findViewById(R.id.tvStatus);
        tvLogs         = findViewById(R.id.tvLogs);
        etOtp          = findViewById(R.id.etOtp);
        btnSendOtp     = findViewById(R.id.btnSendOtp);
        btnVerifyOtp   = findViewById(R.id.btnVerifyOtp);
        btnStartService= findViewById(R.id.btnStartService);
        btnSettings    = findViewById(R.id.btnSettings);

        btnSettings.setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnStartService.setOnClickListener(v -> startRubikaService());

        requestPermissions();
        registerRubikaReceiver();
        refreshStatus();
    }

    private void sendOtp() {
        SharedPreferences prefs = getSharedPreferences("dailytask", MODE_PRIVATE);
        String phone = prefs.getString("rubika_phone", "");
        if (phone.isEmpty()) {
            toast("ابتدا شماره روبیکا را در تنظیمات وارد کن");
            return;
        }
        pendingPhone = phone;
        tvStatus.setText("ارسال OTP به روبیکا...");

        Intent intent = new Intent(this, RubikaService.class);
        intent.setAction(RubikaService.ACTION_REQUEST_OTP);
        intent.putExtra(RubikaService.EXTRA_PHONE, phone);
        startForegroundService(intent);
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        if (otp.isEmpty()) { toast("کد OTP را وارد کن"); return; }
        if (pendingPhone == null) { toast("اول OTP بخواه"); return; }

        tvStatus.setText("بررسی کد...");
        Intent intent = new Intent(this, RubikaService.class);
        intent.setAction(RubikaService.ACTION_VERIFY_OTP);
        intent.putExtra(RubikaService.EXTRA_PHONE, pendingPhone);
        intent.putExtra(RubikaService.EXTRA_OTP, otp);
        startForegroundService(intent);
    }

    private void startRubikaService() {
        Intent intent = new Intent(this, RubikaService.class);
        startForegroundService(intent);
        tvStatus.setText("سرویس در حال اجرا — هر ۵ دقیقه همگام می‌شه");
    }

    private void refreshStatus() {
        SharedPreferences prefs = getSharedPreferences("dailytask", MODE_PRIVATE);
        String phone = prefs.getString("rubika_phone", "(تنظیم نشده)");
        String owner = prefs.getString("gh_owner", "");
        String repo  = prefs.getString("gh_repo",  "");
        String ghOk  = (!owner.isEmpty() && !repo.isEmpty()) ? owner + "/" + repo : "تنظیم نشده";
        tvStatus.setText("روبیکا: " + phone + "\nGitHub: " + ghOk);
    }

    private void registerRubikaReceiver() {
        rubikaReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                String event = intent.getStringExtra("event");
                String data  = intent.getStringExtra("data");
                runOnUiThread(() -> {
                    String current = tvLogs.getText().toString();
                    tvLogs.setText("[" + event + "] " + data + "\n" + current);
                    if ("OTP_SENT".equals(event))     tvStatus.setText("کد OTP فرستاده شد — آن را وارد کن");
                    if ("OTP_VERIFIED".equals(event)) tvStatus.setText("✅ لاگین روبیکا موفق بود");
                    if ("OTP_ERROR".equals(event))    tvStatus.setText("❌ خطا: " + data);
                });
            }
        };
        IntentFilter filter = new IntentFilter("ir.fadavi.dailytask.RUBIKA_EVENT");
        registerReceiver(rubikaReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    private void requestPermissions() {
        String[] perms = {
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms = new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.POST_NOTIFICATIONS,
            };
        }
        ActivityCompat.requestPermissions(this, perms, 100);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() { super.onResume(); refreshStatus(); }

    @Override
    protected void onDestroy() {
        if (rubikaReceiver != null) unregisterReceiver(rubikaReceiver);
        super.onDestroy();
    }
}
