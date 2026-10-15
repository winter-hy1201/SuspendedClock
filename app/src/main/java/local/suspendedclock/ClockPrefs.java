package local.suspendedclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import java.time.ZoneId;

final class ClockPrefs {
    final SharedPreferences values;
    final Context context;
    static final String[] FONTS = {"sans-serif-medium", "sans-serif", "sans-serif-light", "monospace", "serif", "cursive",
            "sans-serif-condensed", "sans-serif-condensed-light", "sans-serif-black", "sans-serif-thin", "serif-monospace",
            "sans-serif-smallcaps", "casual", "sans-serif-condensed-medium", "sans-serif", "custom"};
    static final int[] WIDTHS = {220, 160, 140, 220, 240, 170};
    static final int[] HEIGHTS = {124, 100, 140, 90, 48, 48};
    ClockPrefs(Context context) { this.context = context; values = context.getSharedPreferences("appearance", Context.MODE_PRIVATE); }
    boolean bool(String key, boolean fallback) { return values.getBoolean(key, fallback); }
    int number(String key, int fallback) { return values.getInt(key, fallback); }
    String text(String key, String fallback) { return values.getString(key, fallback); }
    void set(String key, boolean value) { values.edit().putBoolean(key, value).apply(); }
    void set(String key, int value) { values.edit().putInt(key, value).apply(); }
    void set(String key, String value) { values.edit().putString(key, value).apply(); }
    boolean countdown() { return text("mode", "clock").equals("countdown"); }
    void switchMode() {
        if (bool("enableClock", true) && bool("enableCountdown", true)) set("mode", countdown() ? "clock" : "countdown");
        else set("mode", bool("enableClock", true) ? "clock" : "countdown");
    }
    String format() { return text("format", "HMSs"); }
    int digits() { return 1; }
    long interval() { return format().endsWith("s") ? 100L : 200L; }
    int style() { return Math.max(0, Math.min(5, number("windowStyle", 0))); }
    int width() { return Math.round(WIDTHS[style()] * number("scale", 100) / 100f); }
    int height() { return Math.round(HEIGHTS[style()] * number("scale", 100) / 100f); }
    int textColor() { return number("clockText", 0xffffffff); }
    int milliColor() { return number("clockMilli", 0xffff5d42); }
    int background() { return number("clockBackground", 0xff475164); }
    int opacity() { return number("clockOpacity", 70); }
    Typeface font() {
        int index = Math.max(0, Math.min(FONTS.length - 1, number("fontIndex", 0)));
        if (index == FONTS.length - 1) {
            try { return Typeface.createFromFile(new java.io.File(context.getFilesDir(), "clock-font.ttf")); }
            catch (RuntimeException ignored) { return Typeface.create("sans-serif-medium", Typeface.NORMAL); }
        }
        return Typeface.create(FONTS[index], index == 14 ? Typeface.ITALIC : Typeface.NORMAL);
    }
    String display(Countdown state) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (!countdown() || state.finished && text("end", "zero").equals("clock")) {
            return TimeText.clock(System.currentTimeMillis(), ZoneId.systemDefault(), format(), true, digits());
        }
        if (state.finished && text("end", "zero").equals("countup")) {
            long elapsed = state.target ? System.currentTimeMillis() - state.targetWall : now - state.deadline;
            return TimeText.duration(Math.max(0, elapsed), format(), digits(), false);
        }
        return TimeText.duration(state.remainingAt(now), format(), digits(), true);
    }
    void resetColors() { values.edit().remove("clockText").remove("clockMilli").remove("clockBackground").remove("clockOpacity").remove("imageBackground").apply(); }
    void resetFonts() { values.edit().remove("fontIndex").remove("scaleMain").remove("scaleSub").remove("verticalOffset").apply(); }
}
