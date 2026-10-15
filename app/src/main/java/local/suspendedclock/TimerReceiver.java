package local.suspendedclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class TimerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if ("local.suspendedclock.TOGGLE".equals(intent.getAction())) TimerStore.toggle(context);
        else if ("local.suspendedclock.RESTART".equals(intent.getAction())) TimerStore.restart(context);
        else if (!TimerStore.finish(context)) TimerAlarm.schedule(context, TimerStore.read(context));
    }
}
