package ir.fadavi.dailytask;

import android.content.Context;
import android.content.SharedPreferences;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import org.json.JSONObject;

public class GitHubLogger {

    private final Context ctx;

    public GitHubLogger(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences("dailytask", Context.MODE_PRIVATE);
    }

    /** Push any JSON payload to GitHub logs/{date}/{category}.json */
    public boolean push(String category, JSONObject payload) {
        String token = prefs().getString("gh_token", "");
        String owner = prefs().getString("gh_owner", "");
        String repo  = prefs().getString("gh_repo",  "");

        if (token.isEmpty() || owner.isEmpty() || repo.isEmpty()) {
            AppLog.w("GitHubLogger", "credentials not set — skip push");
            return false;
        }

        try {
            Python py = Python.getInstance();
            PyObject mod = py.getModule("github_logger");
            PyObject result = mod.callAttr("push_log", token, owner, repo, category, payload.toString());
            String ok = result.callAttr("get", "ok").toString();
            if (!"True".equals(ok)) {
                String err = result.callAttr("get", "error").toString();
                AppLog.e("GitHubLogger", "push failed: " + err);
                return false;
            }
            AppLog.i("GitHubLogger", "pushed " + category);
            return true;
        } catch (Exception e) {
            AppLog.e("GitHubLogger", "exception: " + e.getMessage());
            return false;
        }
    }
}
