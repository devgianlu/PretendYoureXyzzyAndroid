package com.gianlu.pretendyourexyzzy.CardViews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;

@SuppressLint("ViewConstructor")
public class PyxCardsGroupView extends LinearLayout {
    private final int mCornerRadius;
    private final int mLineWidth;
    private final int mPaddingSmall;
    private final Paint mLinePaint;
    private final CardListener listener;
    private int mPadding;
    private CardsGroup cards;

    public PyxCardsGroupView(Context context, CardListener listener) {
        super(context);
        this.listener = listener;
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);

        mPaddingSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        mCornerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        mLineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.6f, getResources().getDisplayMetrics());

        mLinePaint = new Paint();
        mLinePaint.setARGB(100, 0, 0, 0);
        mLinePaint.setStrokeWidth(mLineWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
    }

    public PyxCardsGroupView(Context context, CardsGroup whiteCards, @Nullable Action action, CardListener listener) {
        this(context, listener);
        setCards(whiteCards, action, false, null);
    }

    public void calcPaddings() {
        mPadding = mPaddingSmall;
        if (cards != null && cards.size() > 1) mPadding = (int) (mPadding * 1.5f);
    }

    public int[] getPaddings(int pos, boolean forGrid, @Nullable RecyclerView.ViewHolder holder) {
        if (forGrid) return new int[]{0, mPaddingSmall / 2, 0, mPaddingSmall / 2};

        int paddingStart;
        if (holder == null) {
            if (pos == 0) paddingStart = mPadding;
            else paddingStart = 0;
        } else {
            if (cards.size() == 1) {
                if (holder.getLayoutPosition() == 0) paddingStart = mPadding;
                else if (pos == 0) paddingStart = 0;
                else paddingStart = mPadding;
            } else {
                if (pos == 0) paddingStart = mPadding;
                else paddingStart = 0;
            }
        }

        int paddingEnd;
        if (pos == cards.size() - 1) paddingEnd = mPadding;
        else paddingEnd = mPaddingSmall;

        return new int[]{paddingStart, mPadding, paddingEnd, mPadding};
    }

    public void setCards(@NonNull final CardsGroup cards, @Nullable Action action, boolean forGrid, @Nullable RecyclerView.ViewHolder holder) {
        this.cards = cards;
        calcPaddings();

        removeAllViews();

        for (int i = 0; i < cards.size(); i++) {
            final BaseCard card = cards.get(i);
            GameCardView pyxCard = new GameCardView(getContext(), card, action, new GameCardView.CardListener() {
                @Override
                public void onDelete() {
                    if (listener != null) listener.onCardAction(Action.DELETE, cards, card);
                }

                @Override
                public void onToggleStar() {
                    if (listener != null) listener.onCardAction(Action.TOGGLE_STAR, cards, card);
                }
            });
            pyxCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onCardAction(Action.SELECT, cards, card);
                }
            });

            addView(pyxCard);

            int[] paddings = getPaddings(i, forGrid, holder);
            LinearLayout.LayoutParams params = (LayoutParams) pyxCard.getLayoutParams();
            params.setMargins(paddings[0], paddings[1], paddings[2], paddings[3]);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cards != null && cards.size() > 1)
            canvas.drawRoundRect(mPadding / 2, mPadding / 2 + mLineWidth / 2, canvas.getWidth() - mPadding / 2, canvas.getHeight() - mPadding / 2, mCornerRadius, mCornerRadius, mLinePaint);
    }

    public enum Action {
        SELECT,
        DELETE,
        TOGGLE_STAR
    }

    public interface CardListener {
        void onCardAction(@NonNull Action action, @NonNull CardsGroup group, @NonNull BaseCard card);
    }
}
