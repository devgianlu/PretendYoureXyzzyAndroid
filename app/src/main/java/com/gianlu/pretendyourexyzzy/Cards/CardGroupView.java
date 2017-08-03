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
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import java.util.List;

@SuppressLint("ViewConstructor")
public class CardGroupView extends LinearLayout implements PyxCard.ICard {
    private final int mPadding;
    private final int mCornerRadius;
    private final int mLineWidth;
    private final Paint mLinePaint;
    private List<? extends BaseCard> cards;
    private Card associatedBlackCard;
    private ICard listener;
    private boolean starred;
    private StarredCardsManager.StarredCard starredCard;

    public CardGroupView(Context context, ICard listener) {
        super(context);
        this.listener = listener;
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

    public CardGroupView(Context context, List<BaseCard> whiteCards, ICard listener) {
        this(context, listener);
        setCards(whiteCards);
    }

    public void setCards(List<? extends BaseCard> cards) {
        this.cards = cards;
        this.starred = associatedBlackCard != null && StarredCardsManager.hasCard(getContext(), getStarredCard());
        this.starredCard = null;

        removeAllViews();
        for (final BaseCard card : cards) {
            PyxCard pyxCard = new PyxCard(getContext(),
                    card,
                    associatedBlackCard != null,
                    starred,
                    this);

            pyxCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onCardSelected(card);
                }
            });
            addView(pyxCard);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cards != null && cards.size() > 1)
            canvas.drawRoundRect(mPadding, mPadding + mLineWidth / 2, canvas.getWidth() - mPadding, canvas.getHeight() - mPadding, mCornerRadius, mCornerRadius, mLinePaint);
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
        for (int i = 0; i < getChildCount(); i++)
            ((PyxCard) getChildAt(i)).setStarred(starred);
    }

    public void setAssociatedBlackCard(Card associatedBlackCard) {
        this.associatedBlackCard = associatedBlackCard;
        this.starredCard = null;
    }

    private StarredCardsManager.StarredCard getStarredCard() {
        if (starredCard == null || starredCard.blackCard == null || starredCard.whiteCards.isEmpty())
            starredCard = new StarredCardsManager.StarredCard(associatedBlackCard, cards);
        return starredCard;
    }

    @Override
    public void handleStar() {
        StarredCardsManager.StarredCard card = getStarredCard();
        if (StarredCardsManager.hasCard(getContext(), card)) {
            StarredCardsManager.removeCard(getContext(), card);
            setStarred(false);
        } else {
            StarredCardsManager.addCard(getContext(), card);
            setStarred(true);
        }
    }

    @Override
    public void deleteCard(StarredCardsManager.StarredCard card) {
        if (listener != null) listener.onDeleteCard(card);
    }

    public void refreshStarState() {
        setStarred(StarredCardsManager.hasCard(getContext(), getStarredCard()));
    }

    public interface ICard {
        void onCardSelected(BaseCard card);

        void onDeleteCard(StarredCardsManager.StarredCard card);
    }
}
