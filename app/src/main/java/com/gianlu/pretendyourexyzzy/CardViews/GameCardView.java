package com.gianlu.pretendyourexyzzy.CardViews;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.FontsManager;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.R;

public class GameCardView extends CardView {
    private final CardListener listener;
    private final PyxCardsGroupView.Action mainAction;
    private BaseCard card;
    private int width;

    public GameCardView(@NonNull Context context) {
        this(context, null, 0);
    }

    public GameCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.listener = null;
        this.mainAction = null;
    }

    public GameCardView(Context context, BaseCard card, @Nullable PyxCardsGroupView.Action mainAction, @Nullable CardListener listener) {
        super(context);
        this.card = card;
        this.mainAction = mainAction;
        this.listener = listener;
        init();
    }

    private void init() {
        removeAllViews();
        if (card == null) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);
        LayoutInflater.from(getContext()).inflate(R.layout.pyx_card, this, true);
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 156, getResources().getDisplayMetrics());
        setCardElevation((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
        setForeground(CommonUtils.resolveAttrAsDrawable(getContext(), android.R.attr.selectableItemBackground));

        LinearLayout content = findViewById(R.id.pyxCard_content);
        LinearLayout unknown = findViewById(R.id.pyxCard_unknown);
        if (card.unknown()) {
            unknown.setVisibility(VISIBLE);
            content.setVisibility(GONE);
        } else {
            unknown.setVisibility(GONE);
            content.setVisibility(VISIBLE);

            if (card instanceof Card && ((Card) card).isWinner())
                setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            else
                setCardBackgroundColor(card.black() ? Color.BLACK : Color.WHITE);

            SuperTextView text = content.findViewById(R.id.pyxCard_text);
            text.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
            text.setTypeface(FontsManager.get().get(getContext(), FontsManager.ROBOTO_MEDIUM));
            TextView watermark = content.findViewById(R.id.pyxCard_watermark);
            watermark.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
            SuperTextView numPick = content.findViewById(R.id.pyxCard_numPick);
            numPick.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
            SuperTextView numDraw = content.findViewById(R.id.pyxCard_numDraw);
            numDraw.setTextColor(card.black() ? Color.WHITE : Color.BLACK);

            text.setHtml(card.text());
            watermark.setText(card.watermark());
            if (card.black()) {
                numPick.setHtml(R.string.numPick, card.numPick());
                if (card.numDraw() > 0) numDraw.setHtml(R.string.numDraw, card.numDraw());
                else numDraw.setVisibility(GONE);
            } else {
                numDraw.setVisibility(GONE);
                numPick.setVisibility(GONE);
            }

            final ImageButton action = content.findViewById(R.id.pyxCard_action);
            if (mainAction == null) {
                action.setVisibility(GONE);
            } else {
                switch (mainAction) {
                    case SELECT:
                        action.setVisibility(GONE);
                        break;
                    case DELETE:
                        action.setVisibility(VISIBLE);
                        action.setImageResource(R.drawable.ic_delete_black_48dp);
                        action.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (listener != null) listener.onDelete();
                            }
                        });
                        break;
                    case TOGGLE_STAR:
                        action.setVisibility(VISIBLE);
                        action.setImageResource(R.drawable.ic_star_black_48dp);
                        action.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (listener != null) listener.onToggleStar();
                            }
                        });
                        break;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec);
    }

    @Nullable
    public BaseCard getCard() {
        return card;
    }

    public void setCard(@Nullable BaseCard card) {
        this.card = card;
        init();
    }

    interface CardListener {
        void onDelete();

        void onToggleStar();
    }
}
