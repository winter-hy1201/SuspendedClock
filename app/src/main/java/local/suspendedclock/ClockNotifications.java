package local.suspendedclock;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class ClockNotifications {
    static final String RUNNING = "clock_running";
    static final String TIMER = "timer_finished";
    private ClockNotifications() {}
    static void channels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(RUNNING, "悬浮时钟运行状态", NotificationManager.IMPORTANCE_LOW));
        NotificationChannel done = new NotificationChannel(TIMER, "倒计时完成", NotificationManager.IMPORTANCE_HIGH);
        done.enableVibration(true);
        manager.createNotificationChannel(done);
    }
    static PendingIntent open(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static Notification running(Context context, boolean locked) {
        channels(context);
        PendingIntent close = PendingIntent.getService(context, 2,
                new Intent(context, ClockService.class).setAction(ClockService.STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent unlock = PendingIntent.getService(context, 3,
                new Intent(context, ClockService.class).setAction(ClockService.UNLOCK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(context, RUNNING)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("离线悬浮时钟正在运行")
                .setContentText(locked ? "浮窗已锁定 · 点击通知打开设置" : "拖动浮窗移动 · 单击显示操作按钮")
                .setContentIntent(open(context)).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "关闭浮窗", close).build());
        if (locked) builder.addAction(new Notification.Action.Builder(null, "解锁浮窗", unlock).build());
        return builder.build();
    }
    static void finished(Context context) {
        channels(context);
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Notification notification = new Notification.Builder(context, TIMER)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("倒计时结束")
                .setContentText("设定的时间已到").setContentIntent(open(context)).setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM).build();
        context.getSystemService(NotificationManager.class).notify(2, notification);
    }
}
