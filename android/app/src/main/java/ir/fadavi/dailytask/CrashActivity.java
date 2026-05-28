package ir.fadavi.dailytask;

import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        String error = getIntent().getStringExtra("error");
        if (error == null) error = "خطای ناشناخته";

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("❌ خطا — اسکرین‌شات بگیر و بفرست");
        title.setTextSize(16f); title.setPadding(0,0,0,16);
        layout.addView(title);

        final TextView tv = new TextView(this);
        tv.setText(error); tv.setTextSize(11f);
        tv.setBackgroundColor(0xFF111111); tv.setTextColor(0xFFFFCC00);
        tv.setPadding(16,16,16,16);
        layout.addView(tv);

        final String finalError = error;
        Button btn = new Button(this);
        btn.setText("کپی متن خطا");
        btn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("err", finalError));
            Toast.makeText(this, "کپی شد ✅", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btn);

        ScrollView sv = new ScrollView(this);
        sv.addView(layout);
        setContentView(sv);
    }
}
