package com.gianlu.pretendyourexyzzy.Cards;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;

import java.util.Iterator;

@SuppressLint("ViewConstructor")
public class PyxCardsGroupView extends LinearLayout {
    private final int mPadding;
    private final int mCornerRadius;
    private final int mCardsMargin;
    private final int mLineWidth;
    private final Paint mLinePaint;
    private CardsGroup<? extends BaseCard> cards;
    private ICard listener;

    public PyxCardsGroupView(Context context, ICard listener) {
        super(context);
        this.listener = listener;
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);

        mPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        mCornerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        mLineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.6f, getResources().getDisplayMetrics());
        mCardsMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        mLinePaint = new Paint();
        mLinePaint.setARGB(100, 0, 0, 0);
        mLinePaint.setStrokeWidth(mLineWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
    }

    public PyxCardsGroupView(Context context, CardsGroup<? extends BaseCard> whiteCards, ICard listener) {
        this(context, listener);
        setCards(whiteCards);
    }

    public void setIsFirstOfParent(boolean firstOfParent) {
        if (getChildCount() == 0) return;
        if (cards.size() == 1)
            ((LayoutParams) getChildAt(0).getLayoutParams()).leftMargin = firstOfParent ? mCardsMargin : mCardsMargin / 2;
    }

    public void setIsLastOfParent(boolean lastOfParent) {
        if (getChildCount() == 0) return;
        if (cards.size() == 1)
            ((LayoutParams) getChildAt(0).getLayoutParams()).rightMargin = lastOfParent ? mCardsMargin : mCardsMargin / 2;
    }

    public void setCards(CardsGroup<? extends BaseCard> cards) {
        this.cards = cards;

        removeAllViews();

        Iterator<? extends BaseCard> iterator = cards.iterator();
        while (iterator.hasNext()) {
            final BaseCard card = iterator.next();

            PyxCard pyxCard = new PyxCard(getContext(), card);
            pyxCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onCardSelected(card);
                }
            });

            addView(pyxCard);

            if (cards.size() == 1)
                ((LayoutParams) pyxCard.getLayoutParams()).setMargins(mCardsMargin / 2, mCardsMargin, mCardsMargin / 2, mCardsMargin);
            else
                ((LayoutParams) pyxCard.getLayoutParams()).setMargins(mCardsMargin, mCardsMargin, iterator.hasNext() ? 0 : mCardsMargin, mCardsMargin);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cards != null && cards.size() > 1)
            canvas.drawRoundRect(mPadding, mPadding + mLineWidth / 2, canvas.getWidth() - mPadding, canvas.getHeight() - mPadding, mCornerRadius, mCornerRadius, mLinePaint);
    }

    public interface ICard {
        void onCardSelected(BaseCard card);
    }
}
