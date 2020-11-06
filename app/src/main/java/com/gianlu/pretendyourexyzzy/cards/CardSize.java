package com.gianlu.pretendyourexyzzy.cards;

import android.content.Context;
import android.util.TypedValue;

import org.jetbrains.annotations.NotNull;

public enum CardSize {
    SMALL(120, 165), REGULAR(160, 220);

    final int width;
    final int height;

    CardSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int widthPx(@NotNull Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, context.getResources().getDisplayMetrics());
    }

    public int heightPx(@NotNull Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, context.getResources().getDisplayMetrics());
    }
}
