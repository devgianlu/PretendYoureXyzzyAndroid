package com.gianlu.pretendyourexyzzy.CardViews;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.FontsManager;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.R;

public class GameCardView extends CardView {
    public static final int WIDTH_DIP = 156;
    private final CardListener listener;
    private final Action mainAction;
    private final int width;
    private final SuperTextView text;
    private final FrameLayout notText;
    private final ProgressBar loading;
    private final View unknown;
    private final ImageView image;
    private final SuperTextView numDraw;
    private final SuperTextView numPick;
    private final TextView watermark;
    private final ImageButton action;
    private BaseCard card;
    private TextSelectionListener textSelectionListener;

    public GameCardView(@NonNull Context context) {
        this(context, null, 0);
    }

    public GameCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, null, null, null);
    }

    public GameCardView(@NonNull Context context, @Nullable BaseCard card, @Nullable Action mainAction, @Nullable CardListener listener) {
        this(context, null, 0, card, mainAction, listener);
    }

    private GameCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, final BaseCard card, Action mainAction, final CardListener listener) {
        super(context, attrs, defStyleAttr);
        this.card = card;
        this.mainAction = mainAction;
        this.listener = listener;

        LayoutInflater.from(getContext()).inflate(R.layout.pyx_card, this, true);
        width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, WIDTH_DIP, getResources().getDisplayMetrics());
        setCardElevation((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
        setForeground(CommonUtils.resolveAttrAsDrawable(getContext(), android.R.attr.selectableItemBackground));
        setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // FIXME: Not clickable with selection enabled

                if (listener != null)
                    listener.onCardAction(GameCardView.Action.SELECT, card);
            }
        });

        text = findViewById(R.id.pyxCard_text);
        text.setTypeface(FontsManager.get().get(getContext(), FontsManager.ROBOTO_MEDIUM));
        text.setCustomSelectionActionModeCallback(new TextSelectionCallback());

        watermark = findViewById(R.id.pyxCard_watermark);
        numPick = findViewById(R.id.pyxCard_numPick);
        numDraw = findViewById(R.id.pyxCard_numDraw);
        action = findViewById(R.id.pyxCard_action);

        notText = findViewById(R.id.pyxCard_notText);
        loading = notText.findViewById(R.id.pyxCard_loading);
        image = notText.findViewById(R.id.pyxCard_image);
        unknown = notText.findViewById(R.id.pyxCard_unknown);

        init();
    }

    public void setTextSelectionListener(TextSelectionListener textSelectionListener) {
        this.textSelectionListener = textSelectionListener;
    }

    private void showUnknown() {
        text.setVisibility(GONE);
        numPick.setVisibility(GONE);
        numDraw.setVisibility(GONE);
        watermark.setVisibility(GONE);
        notText.setVisibility(VISIBLE);
        unknown.setVisibility(VISIBLE);
        image.setVisibility(GONE);
        loading.setVisibility(GONE);
        action.setVisibility(GONE);
    }

    private void setupAction() {
        if (mainAction != null) {
            switch (mainAction) {
                case SELECT:
                    break;
                case DELETE:
                    action.setVisibility(VISIBLE);
                    action.setImageResource(R.drawable.ic_delete_black_48dp);
                    action.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (listener != null)
                                listener.onCardAction(Action.DELETE, card);
                        }
                    });
                    return;
                case TOGGLE_STAR:
                    action.setVisibility(VISIBLE);
                    action.setImageResource(R.drawable.ic_star_black_48dp);
                    action.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (listener != null)
                                listener.onCardAction(Action.TOGGLE_STAR, card);
                        }
                    });
                    return;
            }

            if (card.getImageUrl() != null) {
                setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (listener != null) {
                            listener.onCardAction(Action.SELECT_IMG, card);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
            } else {
                action.setVisibility(GONE);
            }
        } else {
            if (card.getImageUrl() != null) {
                action.setVisibility(VISIBLE);
                action.setImageResource(R.drawable.ic_zoom_in_black_48dp);
                action.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) listener.onCardAction(Action.SELECT_IMG, card);
                    }
                });
            } else {
                action.setVisibility(GONE);
            }
        }
    }

    private void setupColors() {
        if (card instanceof Card && ((Card) card).isWinner())
            setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        else
            setCardBackgroundColor(card.black() ? Color.BLACK : Color.WHITE);

        text.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
        watermark.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
        numPick.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
        numDraw.setTextColor(card.black() ? Color.WHITE : Color.BLACK);
    }

    private void setupImageCard() {
        text.setVisibility(GONE);
        numDraw.setVisibility(GONE);
        numPick.setVisibility(GONE);

        notText.setVisibility(VISIBLE);
        unknown.setVisibility(GONE);
        loading.setVisibility(VISIBLE);

        image.setVisibility(VISIBLE); // Must be so or Glide won't load
        Glide.with(this).load(card).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException ex, Object model, Target<Drawable> target, boolean isFirstResource) {
                notText.setVisibility(GONE);
                text.setVisibility(VISIBLE);
                text.setTextSize(13);
                text.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
                text.setText(R.string.failedLoadingImage);
                Logging.log(ex);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                text.setVisibility(GONE);
                notText.setVisibility(VISIBLE);
                loading.setVisibility(GONE);
                unknown.setVisibility(GONE);
                image.setVisibility(VISIBLE);
                return false;
            }
        }).into(image);
    }

    private void setupTextCard() {
        text.setVisibility(VISIBLE);
        notText.setVisibility(GONE);

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(text, 8, 18, 1, TypedValue.COMPLEX_UNIT_SP);
        text.setHtml(card.text());

        if (card.black()) {
            numPick.setHtml(R.string.numPick, card.numPick());
            numPick.setVisibility(VISIBLE);

            if (card.numDraw() > 0) {
                numDraw.setHtml(R.string.numDraw, card.numDraw());
                numDraw.setVisibility(VISIBLE);
            } else {
                numDraw.setVisibility(GONE);
            }
        } else {
            numDraw.setVisibility(GONE);
            numPick.setVisibility(GONE);
        }
    }

    private void init() {
        if (card == null) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);

        if (card.unknown()) {
            showUnknown();
        } else {
            setupAction();
            setupColors();

            watermark.setText(card.watermark());

            if (card.getImageUrl() != null) setupImageCard();
            else setupTextCard();
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

    public enum Action {
        SELECT,
        DELETE,
        TOGGLE_STAR,
        SELECT_IMG
    }

    interface CardListener extends TextSelectionListener {
        void onCardAction(@NonNull Action action, @NonNull BaseCard card);
    }

    public interface TextSelectionListener {
        void onTextSelected(@NonNull String text);
    }

    private class TextSelectionCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.card_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.cardSelection_definition:
                    String selection = text.getText().toString().substring(text.getSelectionStart(), text.getSelectionEnd());
                    if (textSelectionListener != null) {
                        textSelectionListener.onTextSelected(selection);
                        return true;
                    } else if (listener != null) {
                        listener.onTextSelected(selection);
                        return true;
                    } else {
                        return false;
                    }
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }
    }
}
