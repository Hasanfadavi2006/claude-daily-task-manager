package ir.fadavi.dailytask;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitHubLogger {
    private static final String TAG = "GHLogger";
    private final SharedPreferences prefs;

    public GitHubLogger(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences("dailytask", Context.MODE_PRIVATE);
    }

    public boolean push(String category, JSONObject payload) {
        String token = prefs.getString("gh_token", "");
        String owner = prefs.getString("gh_owner", "");
        String repo  = prefs.getString("gh_repo",  "");
        if (token.isEmpty() || owner.isEmpty() || repo.isEmpty()) return false;
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String ts   = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
            String path = "logs/" + date + "/" + category + ".json";
            String api  = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;

            JSONArray arr = new JSONArray();
            String sha = null;
            try {
                HttpURLConnection c = open(api, token);
                if (c.getResponseCode() == 200) {
                    JSONObject d = new JSONObject(read(c.getInputStream()));
                    sha = d.optString("sha", null);
                    arr = new JSONArray(new String(Base64.decode(
                        d.getString("content").replace("\n",""), Base64.DEFAULT), "UTF-8"));
                }
                c.disconnect();
            } catch (Exception ignored) {}

            payload.put("ts", ts);
            arr.put(payload);

            JSONObject body = new JSONObject()
                .put("message", "log:" + category + "@" + ts)
                .put("content", Base64.encodeToString(arr.toString(2).getBytes("UTF-8"), Base64.NO_WRAP));
            if (sha != null) body.put("sha", sha);

            HttpURLConnection c = open(api, token);
            c.setRequestMethod("PUT");
            c.setDoOutput(true);
            c.getOutputStream().write(body.toString().getBytes("UTF-8"));
            int code = c.getResponseCode(); c.disconnect();
            Log.i(TAG, category + " → " + code);
            return code == 200 || code == 201;
        } catch (Exception e) { Log.e(TAG, e.getMessage()); return false; }
    }

    private HttpURLConnection open(String url, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        c.setConnectTimeout(15000); c.setReadTimeout(20000);
        return c;
    }

    private String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder(); String l;
        while ((l = br.readLine()) != null) sb.append(l);
        return sb.toString();
    }
}
