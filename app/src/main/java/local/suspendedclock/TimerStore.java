package local.suspendedclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

final class TimerStore {
    private TimerStore() {}
    static synchronized Countdown read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("timer", Context.MODE_PRIVATE);
        Countdown state = new Countdown(prefs.getLong("duration", 300_000));
        state.remaining = prefs.getLong("remaining", state.duration);
        state.deadline = prefs.getLong("deadline", 0);
        state.running = prefs.getBoolean("running", false);
        state.finished = prefs.getBoolean("finished", false);
        state.target = prefs.getBoolean("target", false);
        state.targetWall = prefs.getLong("targetWall", 0);
        state.alignTarget(SystemClock.elapsedRealtime(), System.currentTimeMillis());
        int currentBoot = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        state.boot = prefs.getInt("boot", currentBoot);
        if (state.boot != currentBoot) {
            state.reset();
            state.boot = currentBoot;
            save(context, state);
        }
        return state;
    }

    static synchronized void save(Context context, Countdown state) {
        context.getSharedPreferences("timer", Context.MODE_PRIVATE).edit()
                .putLong("duration", state.duration).putLong("remaining", state.remaining)
                .putLong("deadline", state.deadline).putBoolean("running", state.running)
                .putBoolean("finished", state.finished).putInt("boot", state.boot)
                .putBoolean("target", state.target).putLong("targetWall", state.targetWall).apply();
        TimerAlarm.schedule(context, state);
    }

    static synchronized void start(Context context, long duration) {
        Countdown state = read(context);
        state.target = false;
        state.duration = duration;
        state.reset();
        state.start(SystemClock.elapsedRealtime());
        save(context, state);
    }

    static synchronized void startTarget(Context context, long targetWall) {
        Countdown state = read(context);
        state.target = true;
        state.targetWall = targetWall;
        state.duration = Math.max(1, targetWall - System.currentTimeMillis());
        state.reset();
        state.start(SystemClock.elapsedRealtime());
        save(context, state);
    }

    static synchronized void toggle(Context context) {
        if (finish(context)) return;
        Countdown state = read(context);
        if (state.target) return;
        if (state.running) state.pause(SystemClock.elapsedRealtime());
        else state.start(SystemClock.elapsedRealtime());
        save(context, state);
    }

    static synchronized void restart(Context context) {
        Countdown state = read(context);
        if (state.target) startTarget(context, state.targetWall);
        else start(context, state.duration);
    }

    static synchronized void reset(Context context) {
        Countdown state = read(context);
        state.reset();
        save(context, state);
    }

    static synchronized boolean finish(Context context) {
        Countdown state = read(context);
        if (!state.finishIfDue(SystemClock.elapsedRealtime())) return false;
        save(context, state);
        ClockNotifications.finished(context);
        return true;
    }
}
