package com.gianlu.pretendyourexyzzy.Adapters;

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
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.R;

public class PyxCard extends FrameLayout {
    private Card card;
    private boolean hasBlackCard;
    private boolean isStarred;
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

    public PyxCard(Context context, Card card, boolean hasBlackCard, boolean isStarred, ICard handler) {
        super(context);
        this.card = card;
        this.winning = card.winning;
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
        else cardView.setCardBackgroundColor(card.numPick != -1 ? Color.BLACK : Color.WHITE);
        SuperTextView text = (SuperTextView) findViewById(R.id.pyxCard_text);
        text.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);
        text.setTypeface(roboto);
        TextView watermark = (TextView) findViewById(R.id.pyxCard_watermark);
        watermark.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);
        SuperTextView numPick = (SuperTextView) findViewById(R.id.pyxCard_numPick);
        numPick.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);
        SuperTextView numDraw = (SuperTextView) findViewById(R.id.pyxCard_numDraw);
        numDraw.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);

        star = (ImageButton) findViewById(R.id.pyxCard_star);

        if (!hasBlackCard || card.text.isEmpty()) {
            star.setVisibility(GONE);
        } else {
            star.setVisibility(VISIBLE);
            star.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.handleStar();
                }
            });

            star.setImageResource(isStarred ? R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
        }

        text.setHtml(card.text);
        watermark.setText(card.watermark);
        if (card.numPick != -1) {
            numPick.setHtml(R.string.numPick, card.numPick);
            if (card.numDraw > 0) numDraw.setHtml(R.string.numDraw, card.numDraw);
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

    public void setWinning() {
        winning = true;
        init();
    }

    public void setCard(@Nullable Card card) {
        this.card = card;
        init();
    }

    public interface ICard {
        void handleStar();
    }
}
