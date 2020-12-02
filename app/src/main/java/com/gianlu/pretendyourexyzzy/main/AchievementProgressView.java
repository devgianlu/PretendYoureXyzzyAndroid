package com.gianlu.pretendyourexyzzy.main;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.gianlu.pretendyourexyzzy.R;

public final class AchievementProgressView extends View {
    private final Paint mCompletePaint;
    private final Paint mIncompletePaint;
    private final Paint valueTextPaint;
    private final TextPaint bottomTextPaint;
    private final int mBarRadius = 16;
    private final int mBarHeight = 30;
    private final Rect iconTextRect = new Rect();
    private final Rect maxTextRect = new Rect();
    private final int mIconW = 80;
    private final int mIconH = 80;
    private final int mTextPaddingTop = 5;
    private final int mDescTextPaddingBottom = 7;
    private String mDesc;
    private Drawable mIconDrawable;
    private int mMinValue;
    private int mMaxValue;
    private int mActualValue;

    public AchievementProgressView(Context context) {
        this(context, null, 0);
    }

    public AchievementProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AchievementProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);

        mCompletePaint = new Paint();
        mCompletePaint.setAntiAlias(true);

        mIncompletePaint = new Paint();
        mIncompletePaint.setAntiAlias(true);

        valueTextPaint = new Paint();
        valueTextPaint.setAntiAlias(true);
        valueTextPaint.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_medium));
        valueTextPaint.setTextSize(35);
        valueTextPaint.setColor(Color.BLACK);

        bottomTextPaint = new TextPaint();
        bottomTextPaint.setAntiAlias(true);
        bottomTextPaint.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_regular));
        bottomTextPaint.setTextSize(35);
        bottomTextPaint.setAlpha(200);
        bottomTextPaint.setColor(Color.BLACK);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AchievementProgressView, defStyleAttr, 0);
        try {
            setCompleteColor(a.getColor(R.styleable.AchievementProgressView_completeColor, Color.GREEN));
            setIncompleteColor(a.getColor(R.styleable.AchievementProgressView_incompleteColor, Color.RED));
            setMinValue(a.getInteger(R.styleable.AchievementProgressView_minValue, 0));
            setMaxValue(a.getInteger(R.styleable.AchievementProgressView_maxValue, 100));
            setActualValue(a.getInteger(R.styleable.AchievementProgressView_actualValue, 30));
            setDesc(a.getText(R.styleable.AchievementProgressView_desc));

            Drawable drawable = a.getDrawable(R.styleable.AchievementProgressView_icon);
            if (drawable != null) setIconDrawable(drawable);
        } finally {
            a.recycle();
        }
    }

    public void setIncompleteColor(@ColorInt int incompleteColor) {
        this.mIncompletePaint.setColor(incompleteColor);
        invalidate();
    }

    public void setCompleteColor(@ColorInt int completeColor) {
        this.mCompletePaint.setColor(completeColor);
        invalidate();
    }

    public void setIconDrawable(@NonNull Drawable iconDrawable) {
        this.mIconDrawable = iconDrawable;
        invalidate();
    }

    public void setMinValue(int minValue) {
        this.mMinValue = minValue;
        invalidate();
    }

    public void setMaxValue(int maxValue) {
        this.mMaxValue = maxValue;
        invalidate();
    }

    public void setActualValue(int actualValue) {
        if (actualValue > mMaxValue || actualValue < mMinValue)
            throw new IllegalArgumentException();

        this.mActualValue = actualValue;
        invalidate();
    }

    public void setDesc(@Nullable CharSequence desc) {
        if (desc == null) this.mDesc = null;
        else this.mDesc = desc.toString();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mIconDrawable == null || mDesc == null) return;

        int minX = getPaddingLeft() + mIconW / 2;
        int maxW = getWidth() - getPaddingRight() - mIconW / 2 - minX;

        // Draw desc text
        StaticLayout descTextLayout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            descTextLayout = StaticLayout.Builder.obtain(mDesc, 0, mDesc.length(), bottomTextPaint, maxW)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true)
                    .setLineSpacing(0, 1).setMaxLines(2)
                    .build();
        } else {
            descTextLayout = new StaticLayout(mDesc, 0, mDesc.length(), bottomTextPaint, maxW, Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
        }

        int descTextY = getPaddingTop();

        canvas.save();
        canvas.translate(minX, descTextY);
        descTextLayout.draw(canvas);
        canvas.restore();

        // Draw incomplete bar
        int barTop = getPaddingTop() + descTextLayout.getHeight() + mDescTextPaddingBottom + (mIconH - mBarHeight) / 2;

        canvas.drawRoundRect(minX, barTop, minX + maxW, barTop + mBarHeight, mBarRadius, mBarRadius, mIncompletePaint);

        // Draw complete bar
        float completeFraction = (float) mActualValue / (mMaxValue - mMinValue);
        float completeW = maxW * completeFraction;
        canvas.drawRoundRect(minX, barTop, minX + completeW, barTop + mBarHeight, mBarRadius, mBarRadius, mCompletePaint);

        // Draw icon
        int iconX = (int) (minX + completeW - mIconW / 2);
        int iconY = barTop + mBarHeight / 2 - mIconH / 2;
        mIconDrawable.setBounds(iconX, iconY, iconX + mIconW, iconY + mIconH);
        mIconDrawable.draw(canvas);

        // Draw icon text
        int textTop = barTop + mBarHeight + (mIconH - mBarHeight) / 2 + mTextPaddingTop;

        String iconText = String.valueOf(mActualValue);
        valueTextPaint.getTextBounds(iconText, 0, iconText.length(), iconTextRect);

        int iconTextX = (int) (minX + completeW - iconTextRect.width() / 2);
        int iconTextY = textTop + iconTextRect.height();
        canvas.drawText(iconText, iconTextX, iconTextY, valueTextPaint);

        // Draw max text
        String maxText = String.valueOf(mMaxValue);
        valueTextPaint.getTextBounds(maxText, 0, maxText.length(), maxTextRect);

        int maxTextX = minX + maxW - maxTextRect.width() / 2;
        int maxTextY = textTop + maxTextRect.height();

        iconTextRect.offset(iconTextX, iconTextY);
        maxTextRect.offset(maxTextX, maxTextY);
        if (!Rect.intersects(maxTextRect, iconTextRect))
            canvas.drawText(maxText, maxTextX, maxTextY, valueTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = valueTextPaint.getFontMetrics();
        int textHeight = (int) (fm.descent - fm.ascent);

        fm = bottomTextPaint.getFontMetrics();
        textHeight += (int) (fm.descent - fm.ascent);

        int height = getPaddingTop() + mIconH + mTextPaddingTop + textHeight + mDescTextPaddingBottom + getPaddingBottom();
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
