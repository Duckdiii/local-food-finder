package com.example.cuisine_finder.utils;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetUtils {
    private InsetUtils() {
    }

    public static void applySystemBars(View view, boolean applyTop, boolean applyBottom) {
        if (view == null) return;

        int baseLeft = view.getPaddingLeft();
        int baseTop = view.getPaddingTop();
        int baseRight = view.getPaddingRight();
        int baseBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(
                    baseLeft + bars.left,
                    baseTop + (applyTop ? bars.top : 0),
                    baseRight + bars.right,
                    baseBottom + (applyBottom ? bars.bottom : 0)
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
