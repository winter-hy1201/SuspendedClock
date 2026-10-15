package local.suspendedclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ClockFaceView extends FrameLayout {
    private final ClockPrefs prefs;
    private final UiTheme theme;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ClockTextView time;
    private final TextView title, value;
    private final IconView symbol, edit, pause;
    private final LinearLayout editRow;
    private Countdown state;
    ClockFaceView(Context context, ClockPrefs prefs, UiTheme theme, Runnable picker, Runnable toggle) {
        super(context); this.prefs=prefs; this.theme=theme; setWillNotDraw(false);
        symbol=new IconView(context,"clock",theme.blue);symbol.setPadding(dp(12),dp(12),dp(12),dp(12));symbol.setBackground(UiTheme.shape(theme.fill,dp(24)));addView(symbol);
        time=new ClockTextView(context);time.setGravity(Gravity.CENTER);time.setMaxLines(1);time.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));time.setTextColor(theme.text);time.setFontFeatureSettings("tnum");time.setAutoSizeTextTypeUniformWithConfiguration(14,32,1,android.util.TypedValue.COMPLEX_UNIT_SP);addView(time);
        title=new TextView(context);title.setTextSize(14);title.setTextColor(theme.secondary);title.setGravity(Gravity.CENTER);addView(title);
        editRow=new LinearLayout(context);editRow.setGravity(Gravity.CENTER);editRow.setBackground(UiTheme.press(0x00000000,dp(12)));editRow.setContentDescription("设定倒计时时间");editRow.setOnClickListener(v->picker.run());
        value=new TextView(context);value.setTextSize(16);value.setTextColor(theme.blue);editRow.addView(value);
        edit=new IconView(context,"edit",theme.blue);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(dp(20),dp(20));ep.leftMargin=dp(8);editRow.addView(edit,ep);addView(editRow);
        pause=new IconView(context,"pause",theme.blue);pause.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);pause.setBackground(UiTheme.press(theme.surface,dp(24)));pause.setOnClickListener(v->toggle.run());addView(pause);
        update();
    }
    void update(){
        state=TimerStore.read(getContext());boolean timer=prefs.countdown();
        String text=prefs.display(state);if(!text.contentEquals(time.getText()))time.setText(text);
        String label=timer?(state.target?"倒计时-目标时间":"倒计时-时长"):"设备时间";if(!label.contentEquals(title.getText()))title.setText(label);
        if(timer){String v=state.target?DateTimeFormatter.ofPattern("MM-dd HH:mm:ss",Locale.CHINA).format(Instant.ofEpochMilli(state.targetWall).atZone(ZoneId.systemDefault())):durationLabel(state.duration);if(!v.contentEquals(value.getText()))value.setText(v);}
        symbol.setVisibility(timer?GONE:VISIBLE);editRow.setVisibility(timer?VISIBLE:GONE);pause.setVisibility(timer&&!state.target?VISIBLE:GONE);pause.icon(state.running?"pause":"play");pause.setContentDescription(state.running?"暂停计时":"开始计时");
        requestLayout();invalidate();
    }
    static String durationLabel(long millis){long s=millis/1000;String v="";if(s/3600>0)v+=s/3600+"小时";if(s/60%60>0)v+=s/60%60+"分";if(s%60>0||v.isEmpty())v+=s%60+"秒";return v;}
    @Override protected void onMeasure(int w,int h){int size=Math.min(dp(340),Math.min(MeasureSpec.getSize(w),MeasureSpec.getSize(h)));setMeasuredDimension(size,size);for(int i=0;i<getChildCount();i++){android.view.View v=getChildAt(i);v.measure(MeasureSpec.makeMeasureSpec(v==time||v==title||v==editRow?size-dp(48):dp(48),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(v==time?dp(56):dp(48),MeasureSpec.EXACTLY));}}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){int size=r-l;boolean timer=prefs.countdown();boolean large=getResources().getConfiguration().fontScale>1.3f;place(symbol,size/2,size*(large?.25f:.32f));place(time,size/2,size*(large?(timer?.27f:.49f):(timer?.35f:.53f)));place(title,size/2,size*(large?(timer?.44f:.71f):(timer?.48f:.66f)));place(editRow,size/2,size*(large?.61f:.59f));place(pause,size/2,size*(large?.80f:.75f));}
    private void place(android.view.View v,float x,float y){int w=v.getMeasuredWidth(),h=v.getMeasuredHeight();v.layout(Math.round(x-w/2f),Math.round(y-h/2f),Math.round(x+w/2f),Math.round(y+h/2f));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float mid=getWidth()/2f, radius=mid-dp(5);paint.setStyle(Paint.Style.FILL);paint.setColor(theme.surface);c.drawCircle(mid,mid,radius,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(theme.line);c.drawCircle(mid,mid,radius,paint);
        for(int i=0;i<60;i++){double a=Math.PI*i/30;float outer=radius-dp(10),inner=outer-dp(i%5==0?7:4);paint.setColor(i%5==0?theme.secondary:theme.line);paint.setStrokeWidth(dp(i%5==0?2:1));c.drawLine(mid+(float)Math.sin(a)*inner,mid-(float)Math.cos(a)*inner,mid+(float)Math.sin(a)*outer,mid-(float)Math.cos(a)*outer,paint);}
        float inner=radius-dp(35);paint.setColor(theme.fill);paint.setStrokeWidth(dp(5));c.drawCircle(mid,mid,inner,paint);float fraction=(System.currentTimeMillis()%60000)/60000f;if(prefs.countdown()&&state!=null)fraction=state.duration==0?0:Math.max(0,Math.min(1,state.remainingAt(android.os.SystemClock.elapsedRealtime())/(float)state.duration));paint.setColor(theme.blue);paint.setStrokeCap(Paint.Cap.ROUND);c.drawArc(new RectF(mid-inner,mid-inner,mid+inner,mid+inner),-90,360*fraction,false,paint);paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);for(int i=0;i<4;i++){double a=i*Math.PI/2;c.drawCircle(mid+(float)Math.sin(a)*(radius-dp(11)),mid-(float)Math.cos(a)*(radius-dp(11)),dp(3),paint);}
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
