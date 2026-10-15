package local.suspendedclock;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;

final class UiTheme {
    final boolean dark;
    final int background, surface, text, secondary, line, blue, fill;
    UiTheme(Context context) {
        dark = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        background = dark ? 0xff000000 : 0xfff2f2f7;
        surface = dark ? 0xff1c1c1e : 0xffffffff;
        text = dark ? 0xfff5f5f7 : 0xff1c1c1e;
        secondary = dark ? 0xffa0a0a6 : 0xff63636a;
        line = dark ? 0xff38383a : 0xffe5e5ea;
        blue = dark ? 0xff0a84ff : 0xff007aff;
        fill = dark ? 0xff2c2c2e : 0xffe9e9ee;
    }
    static GradientDrawable shape(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(radius); return drawable;
    }
    static Drawable press(int color, float radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x227f7f7f), shape(color, radius), shape(0xffffffff, radius));
    }
}
