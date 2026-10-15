package local.suspendedclock;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;

/** Optional local feedback. Speech is restricted to an installed voice that requires no network. */
final class ClockAlerts {
    private final Context context;
    private final ClockPrefs prefs;
    private ToneGenerator tone;
    private TextToSpeech speech;
    private boolean speechReady,closed;
    private long lastTick=-1,lastVibration=-1,lastSpoken=-1;
    ClockAlerts(Context context,ClockPrefs prefs){this.context=context;this.prefs=prefs;}
    void update(Countdown state){
        long second=System.currentTimeMillis()/1000;
        if(prefs.bool("secondTick",false)&&second!=lastTick){lastTick=second;try{if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_MUSIC,20);tone.startTone(ToneGenerator.TONE_PROP_BEEP,25);}catch(RuntimeException ignored){}}
        long left=state.remainingAt(SystemClock.elapsedRealtime());if(!prefs.countdown()||!state.running||left<=0)return;
        if(left<=3000&&!prefs.text("vibrate","关闭").equals("关闭")){long unit=prefs.text("vibrate","关闭").equals("每半秒")?500:1000;long phase=left/unit;if(phase!=lastVibration){lastVibration=phase;context.getSystemService(Vibrator.class).vibrate(VibrationEffect.createOneShot(60,VibrationEffect.DEFAULT_AMPLITUDE));}}
        if(left<=5000&&prefs.text("voice","关闭").equals("开启")){
            if(speech==null){speech=new TextToSpeech(context,status->{if(closed)return;Voice selected=null;java.util.Set<Voice> voices=status==TextToSpeech.SUCCESS?speech.getVoices():null;if(voices!=null)for(Voice voice:voices){if(!voice.isNetworkConnectionRequired()&&voice.getLocale().getLanguage().equals(Locale.CHINESE.getLanguage())){selected=voice;break;}}if(selected!=null){speech.setVoice(selected);speechReady=true;}else{prefs.set("voice","关闭");android.widget.Toast.makeText(context,"未检测到离线中文语音，请先安装离线语音引擎",android.widget.Toast.LENGTH_LONG).show();}});}
            long value=(left+999)/1000;if(speechReady&&value!=lastSpoken){lastSpoken=value;speech.speak(Long.toString(value),TextToSpeech.QUEUE_FLUSH,null,"countdown-"+value);}
        }
    }
    void close(){closed=true;if(tone!=null)tone.release();if(speech!=null){speech.stop();speech.shutdown();}}
}
