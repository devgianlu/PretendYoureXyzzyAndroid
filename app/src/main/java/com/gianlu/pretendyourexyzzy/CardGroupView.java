package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.TypedValue;
import android.widget.LinearLayout;

import com.gianlu.pretendyourexyzzy.Adapters.PyxCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import java.util.List;

public class CardGroupView extends LinearLayout {
    private final int mPadding;
    private final int mCornerRadius;
    private final int mLineWidth;
    private final Paint mLinePaint;
    private List<Card> cards;

    public CardGroupView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setWillNotDraw(false);

        mPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, context.getResources().getDisplayMetrics());
        mCornerRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources().getDisplayMetrics());
        mLineWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.6f, context.getResources().getDisplayMetrics());

        mLinePaint = new Paint();
        mLinePaint.setARGB(100, 0, 0, 0);
        mLinePaint.setStrokeWidth(mLineWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;

        removeAllViews();
        for (Card card : cards)
            addView(new PyxCard(getContext(), card));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cards != null && cards.size() > 1)
            canvas.drawRoundRect(mPadding, mPadding + mLineWidth / 2, canvas.getWidth() - mPadding, canvas.getHeight() - mPadding, mCornerRadius, mCornerRadius, mLinePaint);
    }

    public void setWinning() { // FIXME
        for (int i = 0; i < getChildCount(); i++) {
            PyxCard child = (PyxCard) getChildAt(i);
            removeViewAt(i);
            child.setWinning();
            addView(child, i);
        }

        invalidate();
    }
}
