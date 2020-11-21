package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AspectRationFrameLayout extends FrameLayout {
    private final Adjust adjust;
    private final float ratio;

    public AspectRationFrameLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public AspectRationFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AspectRationFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AspectRationFrameLayout, defStyleAttr, 0);
        try {
            adjust = Adjust.values()[a.getInt(R.styleable.AspectRationFrameLayout_adjust, 0)];

            String ratioStr = a.getString(R.styleable.AspectRationFrameLayout_ratio);
            if (ratioStr == null) ratioStr = "1:1";
            String[] split = ratioStr.split(":");
            float first = Float.parseFloat(split[0]);
            float second = Float.parseFloat(split[1]);
            ratio = first / (first + second);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (adjust == Adjust.WIDTH) {
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(MeasureSpec.makeMeasureSpec((int) (height * ratio), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        } else {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec((int) (width / ratio), MeasureSpec.EXACTLY));
        }

        super.onMeasure(getMeasuredWidthAndState(), getMeasuredHeightAndState());
    }

    private enum Adjust {
        WIDTH, HEIGHT
    }
}
