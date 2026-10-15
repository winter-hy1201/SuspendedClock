package local.suspendedclock;

import java.time.Instant;
import java.time.ZoneId;

public final class CoreChecks {
    private static int checks;
    public static void main(String[] args) {
        Countdown timer = new Countdown(10_000);
        timer.start(1000);
        equal(11_000L, timer.deadline, "initial deadline");
        equal(7000L, timer.remainingAt(4000), "elapsed realtime");
        timer.pause(4000);
        equal(7000L, timer.remainingAt(100_000), "pause freezes remaining time");
        timer.start(100_000);
        equal(107_000L, timer.deadline, "resume uses remaining duration");
        timer.start(101_000);
        equal(107_000L, timer.deadline, "repeated start does not move deadline");
        equal(false, timer.finishIfDue(106_999), "not before deadline");
        equal(true, timer.finishIfDue(107_000), "complete at deadline");
        equal(false, timer.finishIfDue(107_001), "complete once");
        equal(0L, timer.remainingAt(200_000), "never negative");
        timer.start(200_000);
        equal(210_000L, timer.deadline, "restart finished timer");
        timer.pause(211_000);
        equal(true, timer.finished, "late pause cannot create negative timer");
        timer.reset();
        equal(10_000L, timer.remainingAt(1_000_000), "reset restores duration");
        equal(false, timer.running, "reset stops timer");
        timer.start(1000);
        equal(true, timer.finishIfDue(90_000), "delayed frame/sleep finishes by deadline");
        Countdown target = new Countdown(30_000);
        target.target = true;
        target.targetWall = 130_000;
        target.start(1000);
        target.alignTarget(1000,100_000);
        equal(30_000L,target.remainingAt(1000),"target uses wall time");
        target.alignTarget(2000,90_000);
        equal(40_000L,target.remainingAt(2000),"setting wall clock earlier extends target timer");
        target.alignTarget(3000,131_000);
        equal(0L,target.remainingAt(3000),"setting wall clock later crosses target");
        equal(true,target.finishIfDue(3000),"crossed target completes");
        target.alignTarget(4000,50_000);
        equal(false,target.running,"completed target cannot restart on wall correction");
        Countdown duration = new Countdown(30_000);
        duration.start(1000);
        duration.alignTarget(2000,999_999);
        equal(29_000L,duration.remainingAt(2000),"duration ignores wall correction");

        equal(-1,MinuteCue.index(49_999),"minute cue hidden before :50");
        equal(0,MinuteCue.index(50_000),"minute cue starts at :50");
        equal(0,MinuteCue.index(50_999),"first marker lasts one second");
        equal(1,MinuteCue.index(51_000),"next marker at :51");
        equal(9,MinuteCue.index(59_999),"last marker before rollover");
        equal(-1,MinuteCue.index(60_000),"minute cue hides at :00");
        equal(-1,MinuteCue.index(60_001),"minute cue stays hidden after rollover");
        equal(0,MinuteCue.index(110_000),"next minute repeats cue");
        for(int i=0;i<10;i++)equal(i,MinuteCue.index(50_000+i*1000),"each of ten one-second markers");

        long wall = Instant.parse("2026-10-15T00:09:08.123Z").toEpochMilli();
        ZoneId utc = ZoneId.of("UTC");
        equal("00:09:08.123", TimeText.clock(wall, utc, "HMSs", true, 3), "millisecond fields");
        equal("08:09:08", TimeText.clock(wall, ZoneId.of("Asia/Shanghai"), "HMS", true, 3), "system timezone conversion");
        equal("12:09:08 AM", TimeText.clock(wall, utc, "HMS", false, 3), "12h midnight");
        equal("12:09:08 PM", TimeText.clock(wall + 43_200_000, utc, "HMS", false, 3), "12h noon");
        equal("00:09", TimeText.clock(wall, utc, "HM", true, 3), "minute format");
        equal("09:08.12", TimeText.clock(wall, utc, "MSs", true, 2), "hundredths");
        equal("08.1", TimeText.clock(wall, utc, "Ss", true, 1), "tenths");
        equal("08", TimeText.clock(wall, utc, "S", true, 3), "seconds only");
        equal("00:00:01", TimeText.duration(1, "HMS", 3, true), "positive countdown rounds upward");
        equal("00:00:00", TimeText.duration(0, "HMS", 3, true), "zero remains zero");
        equal("00:01", TimeText.duration(1, "HM", 3, true), "minute rounds upward");
        equal("100:00:00", TimeText.duration(360_000_000, "HMS", 3, false), "hours do not wrap");
        equal("61:01", TimeText.duration(3_661_000, "MS", 3, false), "minutes do not wrap without hours");
        equal("3661.000", TimeText.duration(3_661_000, "Ss", 3, false), "seconds do not wrap without minutes");
        equal("00.2", TimeText.duration(123, "Ss", 1, true), "tenths countdown rounding");
        equal("00.13", TimeText.duration(123, "Ss", 2, true), "hundredths countdown rounding");
        equal("00.123", TimeText.duration(123, "Ss", 3, true), "millis countdown precision");
        equal("00.000", TimeText.duration(-1, "Ss", 3, false), "clamp negative durations");
        long dst = Instant.parse("2026-03-08T07:00:00Z").toEpochMilli();
        equal("03:00:00", TimeText.clock(dst, ZoneId.of("America/New_York"), "HMS", true, 3), "DST zone rules");
        System.out.println("PASS: " + checks + " core checks");
    }
    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        checks++;
    }
}
