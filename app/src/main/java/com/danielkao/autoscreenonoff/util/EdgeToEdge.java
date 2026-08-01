package com.danielkao.autoscreenonoff.util;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/**
 * Android 16 (API 36) enforces edge-to-edge and ignores the
 * windowOptOutEdgeToEdgeEnforcement opt-out that res/values-v35/styles.xml
 * still uses for Android 15. The Holo decor keeps laying the action bar out
 * below the status bar on its own, but nothing insets the navigation side:
 * a 3-button nav bar would permanently cover the bottom of the settings list.
 * Pad the window content by the bottom/side system-bar insets; the top stays
 * with the decor.
 */
public final class EdgeToEdge {

    private EdgeToEdge() {}

    public static void padSystemBars(Activity activity) {
        if (Build.VERSION.SDK_INT < 36) return;
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });
    }
}
