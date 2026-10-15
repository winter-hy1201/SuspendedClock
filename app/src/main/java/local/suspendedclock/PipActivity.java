package local.suspendedclock;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import java.util.ArrayList;

public final class PipActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    static volatile boolean running;
    private ClockAlerts alerts;
    private ClockCardView card;
    private ClockPrefs prefs;
    private boolean entered;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener=(shared,key)->handler.post(()->{card.refresh();if(isInPictureInPictureMode())setPictureInPictureParams(params());});
    private final Runnable tick=new Runnable(){@Override public void run(){TimerStore.finish(PipActivity.this);card.update();if(!ClockService.running)alerts.update(TimerStore.read(PipActivity.this));handler.postDelayed(this,prefs.interval());}};
    @Override public void onCreate(Bundle saved){super.onCreate(saved);prefs=new ClockPrefs(this);alerts=new ClockAlerts(this,prefs);running=true;card=new ClockCardView(this,prefs,false);setContentView(card);prefs.values.registerOnSharedPreferenceChangeListener(listener);getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&!entered){entered=true;handler.post(this::enter);}}
    private PictureInPictureParams params(){
        float ratio=ClockPrefs.WIDTHS[prefs.style()]/(float)ClockPrefs.HEIGHTS[prefs.style()];ratio=Math.max(.42f,Math.min(2.39f,ratio));
        PictureInPictureParams.Builder builder=new PictureInPictureParams.Builder().setAspectRatio(new Rational(Math.round(ratio*100),100));
        android.graphics.Rect source=new android.graphics.Rect();card.getGlobalVisibleRect(source);builder.setSourceRectHint(source);
        if(android.os.Build.VERSION.SDK_INT>=31)builder.setAutoEnterEnabled(true);
        if(prefs.countdown()){
            ArrayList<RemoteAction> actions=new ArrayList<>();
            actions.add(action("暂停/开始计时","local.suspendedclock.TOGGLE",10));actions.add(action("重新开始","local.suspendedclock.RESTART",11));builder.setActions(actions);
        }
        return builder.build();
    }
    private void enter(){
        try{if(!enterPictureInPictureMode(params())){android.widget.Toast.makeText(this,"请在系统设置中允许画中画",android.widget.Toast.LENGTH_LONG).show();finish();}}
        catch(RuntimeException error){android.widget.Toast.makeText(this,"此设备暂不支持画中画，请使用悬浮窗模式",android.widget.Toast.LENGTH_LONG).show();finish();}
    }
    private RemoteAction action(String label,String action,int request){PendingIntent pending=PendingIntent.getBroadcast(this,request,new Intent(this,TimerReceiver.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new RemoteAction(Icon.createWithResource(this,R.drawable.ic_notification),label,label,pending);}
    @Override public void onPictureInPictureModeChanged(boolean pip,Configuration config){super.onPictureInPictureModeChanged(pip,config);if(!pip&&entered){startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}}
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onStop(){handler.removeCallbacks(tick);super.onStop();if(!isInPictureInPictureMode())finish();}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);card.refresh();}
    @Override protected void onDestroy(){running=false;alerts.close();handler.removeCallbacksAndMessages(null);prefs.values.unregisterOnSharedPreferenceChangeListener(listener);super.onDestroy();}
}
