package com.gianlu.pretendyourexyzzy.Cards;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.R;

public class PyxCard extends FrameLayout {
    private BaseCard card;
    private boolean hasBlackCard;
    private Boolean isStarred;
    private ICard handler;
    private ImageButton star;
    private boolean winning = false;

    public PyxCard(@NonNull Context context) {
        super(context);
    }

    public PyxCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PyxCard(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PyxCard(Context context, BaseCard card, boolean hasBlackCard, @Nullable Boolean isStarred, @Nullable ICard handler) {
        super(context);
        this.card = card;
        this.winning = card.isWinning();
        this.hasBlackCard = hasBlackCard;
        this.isStarred = isStarred;
        this.handler = handler;
        init();
    }

    private void init() {
        removeAllViews();
        if (card == null) return;
        LayoutInflater.from(getContext()).inflate(R.layout.pyx_card, this, true);

        Typeface roboto = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf");
        int colorAccent = ContextCompat.getColor(getContext(), R.color.colorAccent);

        CardView cardView = (CardView) findViewById(R.id.pyxCard_card);
        if (winning) cardView.setCardBackgroundColor(colorAccent);
        else cardView.setCardBackgroundColor(card.getNumPick() != -1 ? Color.BLACK : Color.WHITE);
        SuperTextView text = (SuperTextView) findViewById(R.id.pyxCard_text);
        text.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);
        text.setTypeface(roboto);
        TextView watermark = (TextView) findViewById(R.id.pyxCard_watermark);
        watermark.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);
        SuperTextView numPick = (SuperTextView) findViewById(R.id.pyxCard_numPick);
        numPick.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);
        SuperTextView numDraw = (SuperTextView) findViewById(R.id.pyxCard_numDraw);
        numDraw.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);

        star = (ImageButton) findViewById(R.id.pyxCard_star);

        if (card instanceof StarredCardsManager.StarredCard) {
            star.setVisibility(VISIBLE);
            star.setImageResource(R.drawable.ic_delete_black_48dp);
            star.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (handler != null) handler.deleteCard((StarredCardsManager.StarredCard) card);
                }
            });
        } else {
            if (!hasBlackCard || card.getText().isEmpty() || isStarred == null) {
                star.setVisibility(GONE);
            } else {
                star.setVisibility(VISIBLE);
                star.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (handler != null) handler.handleStar();
                    }
                });

                star.setImageResource(isStarred ? R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
            }
        }

        text.setHtml(card.getText());
        watermark.setText(card.getWatermark());
        if (card.getNumPick() != -1) {
            numPick.setHtml(R.string.numPick, card.getNumPick());
            if (card.getNumDraw() > 0) numDraw.setHtml(R.string.numDraw, card.getNumDraw());
            else numDraw.setVisibility(GONE);
        } else {
            numDraw.setVisibility(GONE);
            numPick.setVisibility(GONE);
        }
    }

    public void setStarred(boolean starred) {
        isStarred = starred;
        star.setImageResource(isStarred ? R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
    }

    public void setCard(@Nullable BaseCard card) {
        this.card = card;
        init();
    }

    public interface ICard {
        void handleStar();

        void deleteCard(StarredCardsManager.StarredCard card);
    }
}
