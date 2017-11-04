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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.R;

public class PyxCard extends FrameLayout {
    private BaseCard card;

    public PyxCard(@NonNull Context context) {
        super(context);
    }

    public PyxCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PyxCard(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PyxCard(Context context, BaseCard card) {
        super(context);
        this.card = card;
        init();
    }

    private void init() {
        removeAllViews();
        if (card == null) return;
        LayoutInflater.from(getContext()).inflate(R.layout.pyx_card, this, true);

        Typeface roboto = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf");
        int colorAccent = ContextCompat.getColor(getContext(), R.color.colorAccent);

        CardView cardView = findViewById(R.id.pyxCard_card);
        LinearLayout content = findViewById(R.id.pyxCard_content);
        LinearLayout unknown = findViewById(R.id.pyxCard_unknown);

        if (card.isUnknown()) {
            unknown.setVisibility(VISIBLE);
            content.setVisibility(GONE);
        } else {
            unknown.setVisibility(GONE);
            content.setVisibility(VISIBLE);

            if (card instanceof Card) {
                if (((Card) card).isWinner())
                    cardView.setCardBackgroundColor(colorAccent);
                else
                    cardView.setCardBackgroundColor(card.getNumPick() != -1 ? Color.BLACK : Color.WHITE);
            }

            SuperTextView text = content.findViewById(R.id.pyxCard_text);
            text.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);
            text.setTypeface(roboto);
            TextView watermark = content.findViewById(R.id.pyxCard_watermark);
            watermark.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);
            SuperTextView numPick = content.findViewById(R.id.pyxCard_numPick);
            numPick.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);
            SuperTextView numDraw = content.findViewById(R.id.pyxCard_numDraw);
            numDraw.setTextColor(card.getNumPick() != -1 ? Color.WHITE : Color.BLACK);

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
    }

    public BaseCard getCard() {
        return card;
    }

    public void setCard(@Nullable BaseCard card) {
        this.card = card;
        init();
    }
}
