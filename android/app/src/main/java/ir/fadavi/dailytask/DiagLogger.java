package ir.fadavi.dailytask;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DiagLogger {
    private static final String TAG = "DiagLogger";

    public static void push(final Context ctx, final String event, final String detail) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    SharedPreferences p = ctx.getSharedPreferences("dailytask", Context.MODE_PRIVATE);
                    String token = p.getString("gh_token", "");
                    String owner = p.getString("gh_owner", "hasanfadavi2006");
                    String repo  = p.getString("gh_repo",  "claude-daily-task-manager");
                    if (token.isEmpty()) return;

                    String ts  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
                    String day = ts.substring(0, 10);
                    String path = "logs/" + day + "/diag.json";
                    String api  = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

                    JSONObject payload = new JSONObject()
                        .put("ts", ts)
                        .put("event", event)
                        .put("detail", detail)
                        .put("device", Build.MODEL)
                        .put("android", Build.VERSION.RELEASE)
                        .put("app_version", "2.1");

                    // Read existing array or create new
                    JSONArray arr = new JSONArray();
                    String sha = null;
                    try {
                        HttpURLConnection c = open(api, token);
                        if (c.getResponseCode() == 200) {
                            JSONObject d = new JSONObject(read(c.getInputStream()));
                            sha = d.optString("sha", null);
                            String content = d.getString("content").replace("\n", "");
                            arr = new JSONArray(new String(Base64.decode(content, Base64.DEFAULT), "UTF-8"));
                        }
                        c.disconnect();
                    } catch (Exception ig) {}

                    arr.put(payload);

                    JSONObject body = new JSONObject()
                        .put("message", "diag:" + event + "@" + ts)
                        .put("content", Base64.encodeToString(arr.toString(2).getBytes("UTF-8"), Base64.NO_WRAP));
                    if (sha != null) body.put("sha", sha);

                    HttpURLConnection c = open(api, token);
                    c.setRequestMethod("PUT");
                    c.setDoOutput(true);
                    c.getOutputStream().write(body.toString().getBytes("UTF-8"));
                    int code = c.getResponseCode();
                    c.disconnect();
                    Log.i(TAG, event + " -> " + code);
                } catch (Exception e) {
                    Log.e(TAG, event + " failed: " + e.getMessage());
                }
            }
        }).start();
    }

    private static HttpURLConnection open(String url, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        return c;
    }

    private static String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = br.readLine()) != null) sb.append(l);
        return sb.toString();
    }
}
