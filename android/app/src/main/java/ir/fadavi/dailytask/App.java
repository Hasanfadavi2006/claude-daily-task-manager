package ir.fadavi.dailytask;

import android.app.Application;
import android.content.Intent;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final App self = this;
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
            for (StackTraceElement e : throwable.getStackTrace())
                sb.append("  at ").append(e).append("\n");
            Throwable cause = throwable.getCause();
            if (cause != null) {
                sb.append("Caused by: ").append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append("\n");
                for (StackTraceElement e : cause.getStackTrace())
                    sb.append("  at ").append(e).append("\n");
            }
            final String err = sb.toString();
            DiagLogger.push(self, "CRASH", err);
            try { Thread.sleep(3000); } catch (Exception ig) {}
            Intent i = new Intent(self, CrashActivity.class);
            i.putExtra("error", err);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            self.startActivity(i);
        });
        DiagLogger.push(this, "app_start", "v4.0");
    }
}
