package ir.fadavi.dailytask;

import org.json.*;
import java.io.*;
import java.net.*;

public class RubikaClient {
    private static final String BASE = "http://127.0.0.1:8765";

    public static JSONObject get(String endpoint) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(BASE + endpoint).openConnection();
            c.setConnectTimeout(5000); c.setReadTimeout(10000);
            if (c.getResponseCode() != 200) return err("HTTP " + c.getResponseCode());
            return new JSONObject(read(c.getInputStream()));
        } catch (ConnectException e) { return err("سرور Termux اجرا نیست"); }
        catch (Exception e) { return err(e.getMessage()); }
    }

    public static JSONObject post(String endpoint, JSONObject body) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(BASE + endpoint).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true); c.setConnectTimeout(5000); c.setReadTimeout(15000);
            c.getOutputStream().write(body.toString().getBytes("UTF-8"));
            int code = c.getResponseCode();
            InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
            return new JSONObject(read(is));
        } catch (ConnectException e) { return err("سرور Termux اجرا نیست"); }
        catch (Exception e) { return err(e.getMessage()); }
    }

    public static boolean isServerRunning() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(BASE + "/ping").openConnection();
            c.setConnectTimeout(2000); c.setReadTimeout(2000);
            int code = c.getResponseCode(); c.disconnect();
            return code == 200;
        } catch (Exception e) { return false; }
    }

    private static String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder(); String l;
        while ((l = br.readLine()) != null) sb.append(l);
        return sb.toString();
    }

    private static JSONObject err(String msg) {
        try { return new JSONObject().put("ok", false).put("error", msg); }
        catch (Exception e) { return new JSONObject(); }
    }
}
