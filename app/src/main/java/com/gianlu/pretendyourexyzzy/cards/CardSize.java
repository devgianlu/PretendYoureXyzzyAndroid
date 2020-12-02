package com.gianlu.pretendyourexyzzy.cards;

import android.content.Context;
import android.util.TypedValue;

import org.jetbrains.annotations.NotNull;

public enum CardSize {
    SMALL(120, 165, 18, 1.08f), REGULAR(160, 220, 20, 1.18f);

    public final int titleSize;
    public final float spacingMultiplier;
    final int width;
    final int height;

    CardSize(int width, int height, int titleSize, float spacingMultiplier) {
        this.width = width;
        this.height = height;
        this.titleSize = titleSize;
        this.spacingMultiplier = spacingMultiplier;
    }

    public int widthPx(@NotNull Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, context.getResources().getDisplayMetrics());
    }

    public int heightPx(@NotNull Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, context.getResources().getDisplayMetrics());
    }
}
