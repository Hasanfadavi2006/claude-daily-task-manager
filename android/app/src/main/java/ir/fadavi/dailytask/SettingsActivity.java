package ir.fadavi.dailytask;

import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etPhone, etGhToken, etGhOwner, etGhRepo;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etPhone   = findViewById(R.id.etPhone);
        etGhToken = findViewById(R.id.etGhToken);
        etGhOwner = findViewById(R.id.etGhOwner);
        etGhRepo  = findViewById(R.id.etGhRepo);
        btnSave   = findViewById(R.id.btnSave);

        SharedPreferences prefs = getSharedPreferences("dailytask", MODE_PRIVATE);
        etPhone.setText(prefs.getString("rubika_phone", ""));
        etGhToken.setText(prefs.getString("gh_token", ""));
        etGhOwner.setText(prefs.getString("gh_owner", "hasanfadavi2006"));
        etGhRepo.setText(prefs.getString("gh_repo", "claude-daily-task-manager"));

        btnSave.setOnClickListener(v -> save(prefs));
    }

    private void save(SharedPreferences prefs) {
        prefs.edit()
            .putString("rubika_phone", etPhone.getText().toString().trim())
            .putString("gh_token",     etGhToken.getText().toString().trim())
            .putString("gh_owner",     etGhOwner.getText().toString().trim())
            .putString("gh_repo",      etGhRepo.getText().toString().trim())
            .apply();
        Toast.makeText(this, "ذخیره شد ✅", Toast.LENGTH_SHORT).show();
        finish();
    }
}
