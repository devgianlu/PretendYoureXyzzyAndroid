package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MaxHeightFrameLayout extends FrameLayout {
    private final int mMaxHeight;

    public MaxHeightFrameLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public MaxHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MaxHeightFrameLayout, defStyleAttr, 0);
        try {
            mMaxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightFrameLayout_maxHeight, 0);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = Math.min(mMaxHeight, MeasureSpec.getSize(heightMeasureSpec));
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
