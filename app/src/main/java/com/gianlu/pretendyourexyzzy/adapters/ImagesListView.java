package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.typography.FontsManager;
import com.gianlu.pretendyourexyzzy.R;

import org.apmem.tools.layouts.FlowLayout;
import org.jetbrains.annotations.NotNull;

public final class ImagesListView extends FlowLayout {
    private final TextPaint textPaint;
    private final Rect rect = new Rect();
    private final String emptyText;
    private final int textGravity;
    private final int imageSize;

    public ImagesListView(Context context) {
        this(context, null, 0);
    }

    public ImagesListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImagesListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        FontsManager.set(context, textPaint, R.font.roboto_light);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ImagesListView, defStyle, 0);
        try {
            emptyText = a.getString(R.styleable.ImagesListView_emptyText);
            textGravity = a.getInt(R.styleable.ImagesListView_textGravity, 1);
            textPaint.setTextSize(a.getDimension(R.styleable.ImagesListView_textSize, 0));
            textPaint.setColor(a.getColor(R.styleable.ImagesListView_textColor, CommonUtils.resolveAttrAsColor(context, R.attr.colorOnSurface)));
            imageSize = (int) a.getDimension(R.styleable.ImagesListView_imageSize, -1);
            if (imageSize == -1) throw new IllegalArgumentException();
        } finally {
            a.recycle();
        }

        setMinimumHeight((int) Math.max(getMinimumHeight(), textPaint.getTextSize()));
    }

    public <T> void addItem(@NonNull T obj, @NonNull ImageLoader<T> loader) {
        ImageView view = new ImageView(getContext());
        loader.load(view, obj);
        addView(view, new FlowLayout.LayoutParams(imageSize, imageSize));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), MeasureSpec.makeMeasureSpec(Math.max(MeasureSpec.getSize(getMeasuredHeight()), getMinimumHeight()), MeasureSpec.EXACTLY));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (getChildCount() == 0 && emptyText != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            textPaint.getTextBounds(emptyText, 0, emptyText.length(), rect);
            if (textGravity == 0) {
                canvas.drawText(emptyText, getPaddingStart(), ((getHeight() - getPaddingTop() - getPaddingBottom()) + rect.height()) / 2f + getPaddingTop(), textPaint);
            } else {
                canvas.drawText(emptyText, ((getWidth() - getPaddingEnd() - getPaddingStart()) - rect.width()) / 2f + getPaddingStart(),
                        ((getHeight() - getPaddingTop() - getPaddingBottom()) + rect.height()) / 2f + getPaddingTop(), textPaint);
            }
        }
    }

    public interface ImageLoader<T> {
        void load(@NonNull ImageView view, @NotNull T obj);
    }
}
