package local.suspendedclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.time.ZoneId;

/** Shared rendering for the preview, the real overlay and picture-in-picture. */
final class ClockCardView extends FrameLayout {
    private final ClockPrefs prefs;
    private final ClockTextView time;
    private final TextView[] corners = new TextView[4];
    private final TextView watermark;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawingRect = new RectF();
    private Bitmap bitmap;
    private String imagePath = "";
    private long imageModified;
    private boolean sample;
    private boolean markersVisible;
    private long infoSecond = -1;
    ClockCardView(Context context, ClockPrefs prefs, boolean sample) {
        super(context); this.prefs = prefs; this.sample = sample;
        setWillNotDraw(false);
        setClipToOutline(true);
        time = new ClockTextView(context); time.setGravity(Gravity.CENTER); time.setMaxLines(1); time.setIncludeFontPadding(false);
        addView(time);
        for (int i = 0; i < 4; i++) {
            corners[i] = new TextView(context); corners[i].setMaxLines(1); corners[i].setIncludeFontPadding(false);
            addView(corners[i]);
        }
        watermark = new TextView(context); watermark.setGravity(Gravity.CENTER); watermark.setMaxLines(1); addView(watermark);
        refresh();
    }
    void refresh() {
        time.setTypeface(prefs.font()); time.setTextColor(prefs.textColor());
        int max = Math.max(14, Math.round(30 * prefs.number("scaleMain", 100) / 100f));
        time.setAutoSizeTextTypeUniformWithConfiguration(8, max, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        float opacity = prefs.opacity() / 100f;
        int bg = (prefs.background() & 0x00ffffff) | (Math.round(opacity * 255) << 24);
        setBackground(UiTheme.shape(bg, dp(12)));
        String path = prefs.bool("imageBackground", false) ? new java.io.File(getContext().getFilesDir(), "clock-background.png").getAbsolutePath() : "";
        long modified = path.isEmpty() ? 0 : new java.io.File(path).lastModified();
        if (!imagePath.equals(path) || imageModified != modified) { bitmap = path.isEmpty() ? null : BitmapFactory.decodeFile(path); imagePath = path; imageModified = modified; }
        String[] keys = {"topLeft", "topRight", "bottomLeft", "bottomRight"};
        String[] defaults = {"不显示", "时间源", "屏幕刷新率", "不显示"};
        for (int i = 0; i < corners.length; i++) {
            corners[i].setTypeface(prefs.font()); corners[i].setTextColor(prefs.textColor()); corners[i].setAlpha(0.72f);
            corners[i].setTextSize(11 * prefs.number("scaleSub", 100) / 100f);
            String value = prefs.text(keys[i], defaults[i]);
            setChanged(corners[i], corner(value));
            corners[i].setVisibility(prefs.style() == 3 || prefs.style() == 5 || prefs.style() == 4 && i >= 2 ? GONE : VISIBLE);
        }
        setChanged(watermark, prefs.text("watermark", "")); watermark.setTextColor(prefs.textColor()); watermark.setTextSize(11);
        watermark.setAlpha(prefs.number("watermarkAlpha", 30) / 100f);
        String value = sample ? TimeText.duration(86_399_900, prefs.format(), prefs.digits(), false) : prefs.display(TimerStore.read(getContext()));
        setTime(value);
        markersVisible=showMinuteMarkers();
        requestLayout(); invalidate();
    }
    private boolean showMinuteMarkers() {return !prefs.countdown() && prefs.text("progress","点位标记").equals("点位标记") && prefs.text("progressWhen","始终显示").equals("始终显示") && MinuteCue.active(sample?59_900L:System.currentTimeMillis());}
    void update() {
        boolean visible=showMinuteMarkers();if(visible!=markersVisible){markersVisible=visible;requestLayout();}
        if (!sample) setTime(prefs.display(TimerStore.read(getContext())));
        long second = System.currentTimeMillis()/1000;
        if (!sample && second != infoSecond) {
            infoSecond = second;
            String[] keys = {"topLeft","topRight","bottomLeft","bottomRight"};
            String[] defaults = {"不显示","时间源","屏幕刷新率","不显示"};
            for(int i=0;i<4;i++)setChanged(corners[i],corner(prefs.text(keys[i],defaults[i])));
        }
        if (prefs.countdown() || prefs.text("progressWhen", "始终显示").equals("始终显示")) invalidate();
    }
    private void setTime(String value) {
        if (value.contentEquals(time.getText())) return;
        SpannableString styled = new SpannableString(value);
        int point = value.indexOf('.');
        if (point >= 0) styled.setSpan(new ForegroundColorSpan(prefs.milliColor()), point + 1, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        time.setText(styled);
    }
    private String corner(String value) {
        switch (value) {
            case "时间源": return "设备时间";
            case "日期": return TimeText.date(System.currentTimeMillis(), ZoneId.systemDefault());
            case "屏幕刷新率": return Math.round(getDisplay() == null ? 60 : getDisplay().getRefreshRate()) + "Hz";
            case "电量": return getContext().getSystemService(BatteryManager.class).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "%";
            default: return "";
        }
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int pad = dp(10);
        for (TextView corner : corners) corner.measure(MeasureSpec.makeMeasureSpec(Math.max(1, width / 3 - pad), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(dp(24), MeasureSpec.AT_MOST));
        int side = prefs.style() == 4 ? Math.max(corners[0].getMeasuredWidth(),corners[1].getMeasuredWidth()) + dp(4) : 0;
        int textHeight = height - dp(prefs.style() >= 4 ? (markersVisible?28:8) : 30);
        time.measure(MeasureSpec.makeMeasureSpec(Math.max(1, width - pad * 2 - side * 2), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.max(1, textHeight), MeasureSpec.EXACTLY));
        watermark.measure(MeasureSpec.makeMeasureSpec(Math.max(1, width - pad * 2), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp(24), MeasureSpec.AT_MOST));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l, h = b - t, pad = dp(10), offset = dp(prefs.number("verticalOffset", 0));
        int th = time.getMeasuredHeight(), contentHeight=h-(markersVisible&&prefs.style()>=4?dp(24):0);
        int tw=time.getMeasuredWidth();time.layout((w-tw)/2, (contentHeight - th) / 2 + offset, (w+tw)/2, (contentHeight + th) / 2 + offset);
        for (int i = 0; i < 4; i++) {
            TextView view = corners[i]; int x = i % 2 == 0 ? pad : w - pad - view.getMeasuredWidth();
            int y = i < 2 ? dp(5) : h - dp(markersVisible?27:5) - view.getMeasuredHeight();
            if (prefs.style() == 1) { x = (w - view.getMeasuredWidth()) / 2; if (i % 2 == 1) { view.layout(0,0,0,0); continue; } }
            if (prefs.style() == 4) y = (contentHeight - view.getMeasuredHeight()) / 2;
            view.layout(x, y, x + view.getMeasuredWidth(), y + view.getMeasuredHeight());
        }
        watermark.layout(pad, (contentHeight - watermark.getMeasuredHeight()) / 2, w - pad, (contentHeight + watermark.getMeasuredHeight()) / 2);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            paint.setAlpha(Math.round(prefs.opacity() * 2.55f));
            drawingRect.set(0,0,getWidth(),getHeight());canvas.drawBitmap(bitmap, null, drawingRect, paint); paint.setAlpha(255);
        }
        if (sample || prefs.countdown() || prefs.text("progressWhen", "始终显示").equals("始终显示")) {
            Countdown state = TimerStore.read(getContext()); long left = state.remainingAt(android.os.SystemClock.elapsedRealtime());
            float progress = sample ? .72f : prefs.countdown() ? state.duration <= 0 ? 0 : Math.max(0, Math.min(1, left / (float) state.duration)) : (System.currentTimeMillis()%60000)/60000f;
            String style=prefs.text("progress", "点位标记");
            if(!prefs.countdown() && style.equals("点位标记")) {
                if(markersVisible)drawMinuteMarkers(canvas,MinuteCue.index(sample?59_900L:System.currentTimeMillis()));
            } else drawProgress(canvas, progress, style);
            if (prefs.countdown() && state.running && left > 0 && left <= 5000 && !prefs.text("flash", "关闭").equals("关闭") && !sample) {
                paint.setColor((android.os.SystemClock.elapsedRealtime() / 250 % 2 == 0) ? 0x44ffffff : 0x00000000);
                canvas.drawRoundRect(0,0,getWidth(),getHeight(),dp(12),dp(12),paint);
            }
        }
    }
    private void drawMinuteMarkers(Canvas canvas,int current) {
        if(current<0)return;
        float left=dp(12),right=getWidth()-dp(12),y=getHeight()-dp(6),step=(right-left)/(MinuteCue.MARKS-1);
        paint.setStyle(Paint.Style.FILL);paint.setColor(prefs.milliColor());paint.setAlpha(65);
        canvas.drawRect(left,y-dp(1)/2f,right,y+dp(1)/2f,paint);
        paint.setAlpha(255);canvas.drawRect(left,y-dp(1)/2f,left+current*step,y+dp(1)/2f,paint);
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(9));paint.setTypeface(prefs.font());
        for(int i=0;i<MinuteCue.MARKS;i++){
            float x=left+i*step;paint.setColor(prefs.milliColor());paint.setAlpha(i<=current?255:65);
            canvas.drawCircle(x,y,dp(i==current?3:2),paint);
            paint.setColor(prefs.textColor());paint.setAlpha(i==current?255:180);
            canvas.drawText(Integer.toString(MinuteCue.MARKS-i),x,y-dp(7),paint);
        }
        paint.setAlpha(255);
    }
    private void drawProgress(Canvas canvas, float fraction, String style) {
        if (style.equals("不显示")) return;
        paint.setStyle(Paint.Style.FILL);paint.setColor(prefs.milliColor());
        float width=getWidth(),height=getHeight(),position=Math.max(dp(3),Math.min(width-dp(3),width*fraction));
        if(style.equals("顶部")||style.equals("底部")){
            float top=style.equals("顶部")?0:height-dp(3);canvas.drawRect(0,top,width*fraction,top+dp(3),paint);
        }else if(style.equals("点位标记")){
            canvas.drawCircle(position,height-dp(4),dp(3),paint);
        }else if(style.equals("时间点")){
            canvas.drawRect(position-dp(1),height-dp(10),position+dp(1),height,paint);
        }else{
            int count=style.equals("刻度尺")?60:12;
            for(int i=0;i<=count;i++){paint.setAlpha(i/(float)count<=fraction?255:70);float x=i*width/count;canvas.drawRect(x,height-dp(i%5==0?8:4),x+Math.max(1,dp(1)),height,paint);}paint.setAlpha(255);
        }
    }
    private void setChanged(TextView view, String value) { if (!value.contentEquals(view.getText())) view.setText(value); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
