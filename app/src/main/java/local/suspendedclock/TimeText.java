package local.suspendedclock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting has no Android dependency so boundary behavior can be tested on the JVM. */
public final class TimeText {
    public static final String[] FORMATS = {"HMSs", "HMS", "HM", "MSs", "MS", "Ss", "S"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM月dd日 EEEE", Locale.CHINA);
    private TimeText() {}

    public static String clock(long wallMillis, ZoneId zone, String format, boolean hour24, int digits) {
        ZonedDateTime time = Instant.ofEpochMilli(wallMillis).atZone(zone);
        int h = time.getHour();
        String suffix = "";
        if (!hour24 && format.startsWith("H")) {
            suffix = h < 12 ? " AM" : " PM";
            h = h % 12 == 0 ? 12 : h % 12;
        }
        return parts(h, time.getMinute(), time.getSecond(), time.getNano() / 1_000_000, format, digits) + suffix;
    }

    public static String duration(long millis, String format, int digits, boolean ceil) {
        millis = Math.max(0, millis);
        long unit = format.endsWith("s") ? (digits == 1 ? 100 : digits == 2 ? 10 : 1)
                : format.equals("HM") ? 60_000 : 1_000;
        if (ceil && millis % unit != 0) millis += unit - millis % unit;
        long seconds = millis / 1_000;
        long hours = seconds / 3_600;
        // Without an hour field, minutes/seconds accumulate rather than wrapping at 60.
        long minutes = format.startsWith("H") ? seconds / 60 % 60 : seconds / 60;
        long shownSeconds = format.startsWith("S") ? seconds : seconds % 60;
        return parts(hours, minutes, shownSeconds, (int) (millis % 1_000), format, digits);
    }

    private static String parts(long h, long m, long s, int ms, String format, int digits) {
        String result;
        switch (format) {
            case "HM": result = two(h) + ":" + two(m); break;
            case "MS": case "MSs": result = two(m) + ":" + two(s); break;
            case "S": case "Ss": result = two(s); break;
            default: result = two(h) + ":" + two(m) + ":" + two(s);
        }
        if (format.endsWith("s")) {
            digits = Math.max(1, Math.min(3, digits));
            result += "." + String.format(Locale.ROOT, "%03d", ms).substring(0, digits);
        }
        return result;
    }

    private static String two(long value) { return value < 10 ? "0" + value : Long.toString(value); }
    public static String date(long wallMillis, ZoneId zone) { return DATE.format(Instant.ofEpochMilli(wallMillis).atZone(zone)); }
}
