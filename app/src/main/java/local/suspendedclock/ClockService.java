package local.suspendedclock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public final class ClockService extends Service {
    static final String STOP="local.suspendedclock.STOP",UNLOCK="local.suspendedclock.UNLOCK";
    static volatile boolean running;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ClockPrefs prefs;
    private WindowManager manager;
    private WindowManager.LayoutParams window;
    private FrameLayout root,hover;
    private ClockCardView card;
    private ClockAlerts alerts;
    private boolean attached,screenOn,moved,scaling;
    private float downX,downY;
    private int originX,originY,availableWidth,availableHeight;
    private final Runnable hideHover=()->{if(hover!=null)hover.setVisibility(View.GONE);scaling=false;};
    private final Runnable tick=new Runnable(){@Override public void run(){if(!attached||!screenOn)return;if(!Settings.canDrawOverlays(ClockService.this)){stopSelf();return;}TimerStore.finish(ClockService.this);card.update();alerts.update(TimerStore.read(ClockService.this));handler.postDelayed(this,prefs.interval());}};
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(shared,key)->handler.post(this::applyStyle);
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){screenOn=getSystemService(PowerManager.class).isInteractive();if(Intent.ACTION_TIME_CHANGED.equals(i.getAction())){TimerAlarm.schedule(c,TimerStore.read(c));TimerStore.finish(c);}handler.removeCallbacks(tick);if(screenOn)handler.post(tick);}};
    @Override public void onCreate(){
        super.onCreate();prefs=new ClockPrefs(this);startForeground(1,ClockNotifications.running(this,prefs.bool("locked",false)));
        if(!Settings.canDrawOverlays(this)){toast("请先允许显示悬浮窗");stopSelf();return;}
        manager=getSystemService(WindowManager.class);screenOn=getSystemService(PowerManager.class).isInteractive();alerts=new ClockAlerts(this,prefs);
        root=new FrameLayout(this);root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);card=new ClockCardView(this,prefs,false);root.addView(card,new FrameLayout.LayoutParams(-1,-1));createHover();
        root.setContentDescription("悬浮时钟");root.setOnClickListener(v->action(prefs.text("single","显示操作按钮")));
        GestureDetector gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onSingleTapConfirmed(MotionEvent e){if(!moved)root.performClick();return true;}@Override public boolean onDoubleTap(MotionEvent e){action(prefs.text("double","缩放悬浮窗"));return true;}});gestures.setIsLongpressEnabled(false);
        root.setOnTouchListener((v,event)->{
            gestures.onTouchEvent(event);
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:downX=event.getRawX();downY=event.getRawY();originX=window.x;originY=window.y;moved=false;handler.removeCallbacks(hideHover);return true;
                case MotionEvent.ACTION_MOVE:float dx=event.getRawX()-downX,dy=event.getRawY()-downY;if(Math.hypot(dx,dy)>ViewConfiguration.get(this).getScaledTouchSlop())moved=true;if(moved){window.x=originX+Math.round(dx);window.y=originY+Math.round(dy);clamp();updateLayout();}return true;
                case MotionEvent.ACTION_UP:if(moved)prefs.values.edit().putInt("x",window.x).putInt("y",window.y).apply();handler.postDelayed(hideHover,4000);return true;
                case MotionEvent.ACTION_CANCEL:return true;default:return false;
            }
        });
        window=new WindowManager.LayoutParams(dp(prefs.width()),dp(prefs.height()),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        window.gravity=Gravity.TOP|Gravity.LEFT;window.x=prefs.number("x",dp(24));window.y=prefs.number("y",dp(120));
        try{manager.addView(root,window);attached=true;running=true;}catch(RuntimeException error){toast("无法显示浮窗，请检查系统权限");stopSelf();return;}
        prefs.values.registerOnSharedPreferenceChangeListener(listener);
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screenReceiver,filter);
        applyStyle();handler.post(tick);
    }
    private void createHover(){
        hover=new FrameLayout(this);hover.setBackground(UiTheme.shape(0x99404044,dp(12)));hover.setVisibility(View.GONE);root.addView(hover,new FrameLayout.LayoutParams(-1,-1));
        IconView close=hoverButton("close","关闭悬浮窗",()->stopSelf());hover.addView(close,new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.TOP|Gravity.LEFT));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(hoverButton("clock","回到APP并关闭悬浮窗",()->{openApp("home");stopSelf();}),new LinearLayout.LayoutParams(dp(36),dp(40)));
        row.addView(hoverButton("switch","切换显示模式",()->{prefs.switchMode();}),new LinearLayout.LayoutParams(dp(36),dp(40)));
        row.addView(hoverButton("pause","暂停/开始计时",()->TimerStore.toggle(this)),new LinearLayout.LayoutParams(dp(36),dp(40)));
        row.addView(hoverButton("reset","重置计时",()->{Countdown s=TimerStore.read(this);if(s.target&&s.targetWall<=System.currentTimeMillis())openApp("home");else TimerStore.restart(this);}),new LinearLayout.LayoutParams(dp(36),dp(40)));
        hover.addView(row,new FrameLayout.LayoutParams(-2,dp(44),Gravity.BOTTOM|Gravity.LEFT));
        IconView resize=hoverButton("resize","缩放悬浮窗",()->{});FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.BOTTOM|Gravity.RIGHT);hover.addView(resize,rp);
        final float[] startX={0};final int[] startScale={100};resize.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN){startX[0]=event.getRawX();startScale[0]=prefs.number("scale",100);handler.removeCallbacks(hideHover);return true;}if(event.getActionMasked()==MotionEvent.ACTION_MOVE){int scale=Math.round(startScale[0]+(event.getRawX()-startX[0])/dp(ClockPrefs.WIDTHS[prefs.style()])*100);prefs.set("scale",Math.max(50,Math.min(200,scale)));return true;}if(event.getActionMasked()==MotionEvent.ACTION_UP){v.performClick();handler.postDelayed(hideHover,4000);return true;}return true;});
    }
    private IconView hoverButton(String icon,String description,Runnable click){IconView view=new IconView(this,icon,0xffffffff);view.setPadding(dp(8),dp(8),dp(8),dp(8));view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);view.setContentDescription(description);view.setBackground(UiTheme.press(0,dp(10)));view.setOnClickListener(v->{click.run();handler.removeCallbacks(hideHover);handler.postDelayed(hideHover,4000);});return view;}
    private void action(String value){switch(value){case"显示操作按钮":hover.setVisibility(hover.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);handler.removeCallbacks(hideHover);handler.postDelayed(hideHover,4000);break;case"缩放悬浮窗":scaling=true;hover.setVisibility(View.VISIBLE);handler.removeCallbacks(hideHover);break;case"回到APP并关闭悬浮窗":openApp("home");stopSelf();break;case"切换显示模式":prefs.switchMode();break;case"暂停/开始计时":TimerStore.toggle(this);break;case"重置计时":TimerStore.restart(this);break;case"关闭悬浮窗":stopSelf();break;default:break;}}
    private void applyStyle(){
        if(!attached)return;card.refresh();bounds();window.width=Math.min(dp(prefs.width()),availableWidth);window.height=Math.min(Math.round(window.width*(ClockPrefs.HEIGHTS[prefs.style()]/(float)ClockPrefs.WIDTHS[prefs.style()])),availableHeight);
        boolean locked=prefs.bool("locked",false);if(locked)window.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;else window.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        window.alpha=locked?.75f:1f;if(prefs.bool("keepScreen",false))window.flags|=WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;else window.flags&=~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if(Build.VERSION.SDK_INT>=30)window.setFitInsetsTypes(prefs.bool("avoidBars",true)?WindowInsets.Type.systemBars():0);
        clamp();updateLayout();getSystemService(android.app.NotificationManager.class).notify(1,ClockNotifications.running(this,locked));
    }
    private void bounds(){if(Build.VERSION.SDK_INT>=30){android.view.WindowMetrics metrics=manager.getCurrentWindowMetrics();android.graphics.Insets inset=prefs.bool("avoidBars",true)?metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()):android.graphics.Insets.NONE;availableWidth=metrics.getBounds().width()-inset.left-inset.right;availableHeight=metrics.getBounds().height()-inset.top-inset.bottom;}else{DisplayMetrics metrics=new DisplayMetrics();manager.getDefaultDisplay().getMetrics(metrics);availableWidth=metrics.widthPixels;availableHeight=metrics.heightPixels;}}
    private void clamp(){bounds();window.x=Math.max(0,Math.min(window.x,availableWidth-window.width));window.y=Math.max(0,Math.min(window.y,availableHeight-window.height));}
    private void updateLayout(){if(!attached)return;try{manager.updateViewLayout(root,window);}catch(RuntimeException error){stopSelf();}}
    private void openApp(String page){startActivity(new Intent(this,MainActivity.class).putExtra("page",page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}
    @Override public int onStartCommand(Intent i,int flags,int id){if(i!=null&&STOP.equals(i.getAction()))stopSelf();if(i!=null&&UNLOCK.equals(i.getAction()))prefs.set("locked",false);return START_NOT_STICKY;}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);if(attached)handler.post(this::applyStyle);}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(prefs!=null)prefs.values.unregisterOnSharedPreferenceChangeListener(listener);if(alerts!=null)alerts.close();if(attached){try{unregisterReceiver(screenReceiver);manager.removeView(root);}catch(IllegalArgumentException ignored){}}attached=false;running=false;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
