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

public class CardGroupView extends LinearLayout implements PyxCard.ICard {
    private final int mPadding;
    private final int mCornerRadius;
    private final int mLineWidth;
    private final Paint mLinePaint;
    private List<Card> cards;
    private Card associatedBlackCard;

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

    public void setCards(List<Card> cards) {
        this.cards = cards;

        removeAllViews();
        for (Card card : cards)
            addView(new PyxCard(getContext(), card, associatedBlackCard != null, StarredCardsManager.hasCard(getContext(), new StarredCardsManager.StarredCard(associatedBlackCard, cards)), this));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cards != null && cards.size() > 1)
            canvas.drawRoundRect(mPadding, mPadding + mLineWidth / 2, canvas.getWidth() - mPadding, canvas.getHeight() - mPadding, mCornerRadius, mCornerRadius, mLinePaint);
    }

    public void setStarred(boolean starred) {
        for (int i = 0; i < getChildCount(); i++)
            ((PyxCard) getChildAt(i)).setStarred(starred);
    }

    public void setAssociatedBlackCard(Card associatedBlackCard) {
        this.associatedBlackCard = associatedBlackCard;
    }

    @Override
    public void handleStar() {
        StarredCardsManager.StarredCard card = new StarredCardsManager.StarredCard(associatedBlackCard, cards);
        if (StarredCardsManager.hasCard(getContext(), card)) {
            StarredCardsManager.removeCard(getContext(), card);
            setStarred(false);
        } else {
            StarredCardsManager.addCard(getContext(), card);
            setStarred(true);
        }
    }
}
