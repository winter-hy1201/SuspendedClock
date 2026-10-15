package local.suspendedclock;

/** The ten one-second marks immediately before the next wall-clock minute. */
final class MinuteCue {
    static final int MARKS = 10;
    private MinuteCue() {}
    static int index(long wallMillis) {
        long phase = Math.floorMod(wallMillis, 60_000L);
        return phase < 50_000L ? -1 : (int) ((phase - 50_000L) / 1_000L);
    }
    static boolean active(long wallMillis) { return index(wallMillis) >= 0; }
}
