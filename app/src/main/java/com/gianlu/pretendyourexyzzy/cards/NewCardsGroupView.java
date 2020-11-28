package com.gianlu.pretendyourexyzzy.cards;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import java.util.List;

public class NewCardsGroupView extends LinearLayout {
    private final int mCardHeight;
    private final int mCardWidth;
    private final int mCardMargin;
    private final int mLineWidth;
    private final int mCornerRadius;
    private final Paint mLinePaint;
    private List<BaseCard> cards;
    private boolean selectable = true;

    public NewCardsGroupView(Context context) {
        this(context, null, 0);
    }

    public NewCardsGroupView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NewCardsGroupView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);

        mCardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 230, getResources().getDisplayMetrics());
        mCardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180, getResources().getDisplayMetrics());
        mCardMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        mLineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        mCornerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        mLinePaint = new Paint();
        mLinePaint.setColor(ContextCompat.getColor(context, R.color.appColor_400));
        mLinePaint.setStrokeWidth(mLineWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
    }

    public void setCards(@NonNull CardsGroup group) {
        this.cards = group;

        removeAllViews();
        for (int i = 0; i < group.size(); i++) {
            BaseCard card = group.get(i);
            NewGameCardView cardView = new NewGameCardView(getContext(), true);
            cardView.setCard(card);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mCardWidth, mCardHeight);
            params.topMargin = params.bottomMargin = mCardMargin;

            if (group.size() > 1) params.rightMargin = mCardMargin * 2;
            else params.rightMargin = mCardMargin;

            if (i == 0) params.leftMargin = mCardMargin;

            addView(cardView, params);
        }
    }

    public void setLeftAction(@DrawableRes int iconRes, @Nullable OnClickListener listener) {
        for (int i = 0; i < getChildCount(); i++) {
            int finalI = i;
            ((NewGameCardView) getChildAt(i)).setLeftAction(iconRes, listener == null ? null : v -> listener.onClick(cards.get(finalI)));
        }
    }

    public void setRightAction(@DrawableRes int iconRes, @Nullable OnClickListener listener) {
        for (int i = 0; i < getChildCount(); i++) {
            int finalI = i;
            ((NewGameCardView) getChildAt(i)).setRightAction(iconRes, listener == null ? null : v -> listener.onClick(cards.get(finalI)));
        }
    }

    public void setOnClickListener(@NonNull OnClickListener listener) {
        for (int i = 0; i < getChildCount(); i++) {
            int finalI = i;
            getChildAt(i).setOnClickListener(v -> {
                if (selectable) listener.onClick(cards.get(finalI));
            });
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cards != null && cards.size() > 1)
            canvas.drawRoundRect(mLineWidth / 2f, mLineWidth / 2f, getWidth() - mCardMargin, getHeight() - mLineWidth / 2f,
                    mCornerRadius, mCornerRadius, mLinePaint);
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        for (int i = 0; i < getChildCount(); i++)
            ((NewGameCardView) getChildAt(i)).setSelectable(selectable);
    }

    public interface OnClickListener {
        void onClick(@NonNull BaseCard card);
    }
}
