package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.R;

public final class NavigationView extends View {
    private final Paint bgPaint;
    private final Paint fgPaint;
    private final Paint linePaint;
    private final Paint cardPaint;
    private final int mRadiusAdd = 8;
    private final int mCardWidth = 42;
    private final int mCardHeight = 58;
    private int mSelected = 0;
    private int mRadius;
    private OnSelectionChanged mSelectionChangedListener = null;

    public NavigationView(Context context) {
        this(context, null, 0);
    }

    public NavigationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        bgPaint = new Paint();
        bgPaint.setAntiAlias(true);

        fgPaint = new Paint();
        fgPaint.setAntiAlias(true);

        cardPaint = new Paint();
        cardPaint.setAntiAlias(true);
        cardPaint.setShadowLayer(4, -2, 2, Color.argb(64, 0, 0, 0));

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeWidth(12);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setPathEffect(new DashPathEffect(new float[]{35, 20}, 0)); // TODO: Adjust dynamically based on width

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NavigationView, defStyleAttr, 0);
        try {
            setSelected(a.getInt(R.styleable.NavigationView_selected, 0));
            setForegroundColor(a.getColor(R.styleable.NavigationView_fgColor, Color.RED));
            setBackgroundColor(a.getColor(R.styleable.NavigationView_bgColor, Color.BLACK));
        } finally {
            a.recycle();
        }
    }

    public void setSelected(@IntRange(from = 0, to = 2) int pos) {
        this.mSelected = pos;
        invalidate();
    }

    public void setForegroundColor(@ColorInt int color) {
        this.fgPaint.setColor(color);
        this.linePaint.setColor(color);
        invalidate();
    }

    public void setBackgroundColor(@ColorInt int color) {
        this.bgPaint.setColor(color);
        invalidate();
    }

    public void setOnSelectionChangedListener(@Nullable OnSelectionChanged listener) {
        this.mSelectionChangedListener = listener;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        mRadius = getHeight() / 2 - mRadiusAdd;

        // Draw dashed line
        float lineY = getHeight() / 2f;
        canvas.drawLine((mRadius + mRadiusAdd) * 2, lineY, getWidth() / 2f - mRadius - mRadiusAdd, lineY, linePaint);
        canvas.drawLine(getWidth() / 2f + mRadius + mRadiusAdd, lineY, getWidth() - (mRadius - mRadiusAdd) * 2, lineY, linePaint);

        // Draw selected circle
        drawCircle(canvas, fgPaint, mSelected, mRadiusAdd);

        // Draw all circles
        drawCircle(canvas, bgPaint, 0, 0);
        drawCircle(canvas, bgPaint, 1, 0);
        drawCircle(canvas, bgPaint, 2, 0);

        // Draw cards
        float cardTop = getHeight() / 2f - mCardHeight / 2f;

        float firstCardLeft = mRadius + mRadiusAdd - mCardWidth / 2f;
        drawCard(canvas, firstCardLeft - 5, cardTop - 5, Color.BLACK);
        drawCard(canvas, firstCardLeft + 5, cardTop + 5, Color.WHITE);

        drawCard(canvas, getWidth() / 2f - mCardWidth / 2f, cardTop, Color.BLACK);
        drawCard(canvas, getWidth() - (mRadius + mRadiusAdd) - mCardWidth / 2f, cardTop, Color.WHITE);
    }

    private void drawCircle(@NonNull Canvas canvas, @NonNull Paint paint, int pos, int radiusAdd) {
        float centerX;
        if (pos == 0) centerX = mRadius + mRadiusAdd;
        else if (pos == 1) centerX = getWidth() / 2f;
        else centerX = getWidth() - (mRadius + mRadiusAdd);

        canvas.drawCircle(centerX, getHeight() / 2f, mRadius + radiusAdd, paint);
    }

    private void drawCard(@NonNull Canvas canvas, float left, float top, @ColorInt int color) {
        cardPaint.setColor(color);
        canvas.drawRoundRect(left, top, left + mCardWidth, top + mCardHeight, 4, 4, cardPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN)
            return true;

        if (action == MotionEvent.ACTION_UP) {
            for (int i = 0; i < 3; i++) {
                if (i == mSelected)
                    continue;

                int cy = getHeight() / 2;
                int cx;
                if (i == 0) cx = mRadius + mRadiusAdd;
                else if (i == 1) cx = getWidth() / 2;
                else cx = getWidth() - (mRadius + mRadiusAdd);

                if (Math.sqrt(Math.pow(event.getX() - cx, 2) + Math.pow(event.getY() - cy, 2)) <= mRadius) {
                    mSelected = i;
                    postInvalidate();
                    post(() -> {
                        if (mSelectionChangedListener != null)
                            mSelectionChangedListener.onSelected(mSelected);
                    });
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }

    public interface OnSelectionChanged {
        void onSelected(@IntRange(from = 0, to = 2) int pos);
    }
}
