package ir.fadavi.dailytask;

import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);

        SharedPreferences p = getSharedPreferences("dailytask", MODE_PRIVATE);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etToken = findViewById(R.id.etGhToken);
        EditText etOwner = findViewById(R.id.etGhOwner);
        EditText etRepo  = findViewById(R.id.etGhRepo);

        etPhone.setText(p.getString("rubika_phone",""));
        etToken.setText(p.getString("gh_token",""));
        etOwner.setText(p.getString("gh_owner","hasanfadavi2006"));
        etRepo.setText(p.getString("gh_repo","claude-daily-task-manager"));

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            p.edit()
                .putString("rubika_phone", etPhone.getText().toString().trim())
                .putString("gh_token",     etToken.getText().toString().trim())
                .putString("gh_owner",     etOwner.getText().toString().trim())
                .putString("gh_repo",      etRepo.getText().toString().trim())
                .apply();
            Toast.makeText(this, "ذخیره شد ✅", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
