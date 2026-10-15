package local.suspendedclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

final class IconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String name;
    private int color;
    IconView(Context context, String name, int color) { super(context); this.name = name; this.color = color; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    void icon(String name) { this.name = name; invalidate(); }
    void color(int color) { this.color = color; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.save(); float width=getWidth()-getPaddingLeft()-getPaddingRight(),height=getHeight()-getPaddingTop()-getPaddingBottom();
        float s = Math.min(width,height) / 24f;
        c.translate(getPaddingLeft()+(width-24*s)/2,getPaddingTop()+(height-24*s)/2); c.scale(s,s);
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        Path p = new Path();
        switch (name) {
            case "back": p.moveTo(15,5); p.lineTo(8,12); p.lineTo(15,19); c.drawPath(p,paint); break;
            case "next": p.moveTo(9,5); p.lineTo(16,12); p.lineTo(9,19); c.drawPath(p,paint); break;
            case "up": p.moveTo(6,15); p.lineTo(12,9); p.lineTo(18,15); c.drawPath(p,paint); break;
            case "down": p.moveTo(6,9); p.lineTo(12,15); p.lineTo(18,9); c.drawPath(p,paint); break;
            case "close": c.drawLine(6,6,18,18,paint); c.drawLine(18,6,6,18,paint); break;
            case "pause": paint.setStrokeWidth(3); c.drawLine(9,6,9,18,paint); c.drawLine(15,6,15,18,paint); break;
            case "play": p.moveTo(8,5); p.lineTo(19,12); p.lineTo(8,19); p.close(); c.drawPath(p,paint); break;
            case "edit": p.moveTo(5,16); p.lineTo(16,5); p.lineTo(20,9); p.lineTo(9,20); p.lineTo(4,21); p.close(); c.drawPath(p,paint); break;
            case "countdown": c.drawLine(6,3,18,3,paint); c.drawLine(6,21,18,21,paint); p.moveTo(7,3);p.lineTo(7,7);p.lineTo(16,17);p.lineTo(16,21);p.moveTo(17,3);p.lineTo(17,7);p.lineTo(8,17);p.lineTo(8,21);c.drawPath(p,paint);break;
            case "float": c.drawRoundRect(3,4,21,20,3,3,paint); c.drawRoundRect(12,11,19,17,1,1,paint); break;
            case "style": c.drawRoundRect(3,4,11,20,1,1,paint);c.drawRoundRect(14,4,21,10,1,1,paint);c.drawRoundRect(14,13,21,20,1,1,paint);break;
            case "switch": for(int i=0;i<4;i++){int x=4+i%2*10,y=4+i/2*10;c.drawRoundRect(x,y,x+6,y+6,1,1,paint);}break;
            case "source": for(int i=0;i<3;i++){c.drawRoundRect(3,4+i*6,11,7+i*6,1,1,paint);}c.drawCircle(17,15,5,paint);c.drawLine(17,12,17,16,paint);c.drawLine(17,16,19,17,paint);break;
            case "font": c.drawRoundRect(3,4,21,20,2,2,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(12);paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);c.drawText("Aa",5,16,paint);break;
            case "palette": c.drawCircle(12,12,9,paint);for(int i=0;i<4;i++){double a=i*Math.PI/2;c.drawCircle(12+(float)Math.cos(a)*5,12+(float)Math.sin(a)*5,1,paint);}break;
            case "reset": c.drawArc(new RectF(4,4,20,20),-90,290,false,paint);c.drawLine(12,4,7,4,paint);c.drawLine(7,4,9,8,paint);break;
            case "resize": c.drawLine(4,14,4,20,paint);c.drawLine(4,20,10,20,paint);c.drawLine(14,4,20,4,paint);c.drawLine(20,4,20,10,paint);c.drawLine(5,19,19,5,paint);break;
            case "check": p.moveTo(5,12);p.lineTo(10,17);p.lineTo(19,7);c.drawPath(p,paint);break;
            case "settings": c.drawCircle(12,12,6,paint);c.drawCircle(12,12,2.3f,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*9,12+(float)Math.sin(a)*9,paint);}break;
            default: c.drawCircle(12,12,9,paint);c.drawLine(12,6,12,12,paint);c.drawLine(12,12,16,14,paint);
        }
        c.restore();
    }
}
