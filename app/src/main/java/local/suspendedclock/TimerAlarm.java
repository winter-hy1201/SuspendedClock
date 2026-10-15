package local.suspendedclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class TimerAlarm {
    private TimerAlarm() {}
    static boolean available(Context context) {
        return Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    static void schedule(Context context, Countdown state) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 1,
                new Intent(context, TimerReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pending);
        if (!state.running || !available(context)) return;
        try {
            // RTC follows system wall-clock corrections, including while our process is absent.
            manager.setExactAndAllowWhileIdle(state.target ? AlarmManager.RTC_WAKEUP : AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    state.target ? state.targetWall : state.deadline, pending);
        } catch (SecurityException ignored) {
            // Permission can be revoked between canScheduleExactAlarms() and scheduling.
        }
    }
}
