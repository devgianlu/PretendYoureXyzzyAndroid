package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.R;

public class PyxCard extends FrameLayout {
    private TextView text;
    private TextView watermark;
    private SuperTextView numPick;
    private SuperTextView numDraw;
    private Typeface roboto;
    private Card card;
    private int colorAccent;
    private LinearLayout container;

    public PyxCard(@NonNull Context context) {
        super(context);
    }

    public PyxCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PyxCard(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PyxCard(Context context, Card card) {
        super(context);
        this.card = card;
        init();
    }

    private void init() {
        removeAllViews();
        if (card == null) return;
        LayoutInflater.from(getContext()).inflate(R.layout.pyx_card, this, true);

        roboto = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf");
        colorAccent = ContextCompat.getColor(getContext(), R.color.colorAccent);

        container = (LinearLayout) findViewById(R.id.pyxCard_container);
        container.setBackgroundColor(card.numPick != -1 ? Color.BLACK : Color.WHITE);
        text = (TextView) findViewById(R.id.pyxCard_text);
        text.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);
        text.setTypeface(roboto);
        watermark = (TextView) findViewById(R.id.pyxCard_watermark);
        watermark.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);
        numPick = (SuperTextView) findViewById(R.id.pyxCard_numPick);
        numPick.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);
        numDraw = (SuperTextView) findViewById(R.id.pyxCard_numDraw);
        numDraw.setTextColor(card.numPick != -1 ? Color.WHITE : Color.BLACK);

        text.setText(card.getEscapedText());
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

    public void setWinning() {
        container.setBackgroundColor(colorAccent);
    }

    public Card getCard() {
        return card;
    }

    public void setCard(@Nullable Card card) {
        this.card = card;
        init();
    }
}
