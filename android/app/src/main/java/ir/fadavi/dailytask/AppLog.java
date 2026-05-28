package ir.fadavi.dailytask;

import android.util.Log;

/** Thin wrapper so we can swap Log impl later. */
public class AppLog {
    public static void i(String tag, String msg) { Log.i(tag, msg); }
    public static void w(String tag, String msg) { Log.w(tag, msg); }
    public static void e(String tag, String msg) { Log.e(tag, msg); }
}
