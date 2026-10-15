package local.suspendedclock;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

/** Keep time readable on focus without flooding accessibility with 30 text changes each second. */
final class ClockTextView extends TextView {
    ClockTextView(Context context) { super(context); }
    @Override public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;
        super.sendAccessibilityEventUnchecked(event);
    }
}
