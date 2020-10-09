package com.gianlu.pretendyourexyzzy.cards;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.UnknownCard;
import com.google.android.material.card.MaterialCardView;

public class GameCardView extends MaterialCardView {
    public static final int WIDTH_DIP = 156;
    private static final String TAG = GameCardView.class.getSimpleName();
    public final ImageButton primaryAction;
    public final SuperTextView text;
    private final CardListener listener;
    private final int mWidth;
    private final FrameLayout notText;
    private final ProgressBar loading;
    private final ImageView unknown;
    private final ImageView image;
    private final SuperTextView numDraw;
    private final SuperTextView numPick;
    private final TextView watermark;
    private final ImageButton secondaryAction;
    private final Action primary;
    private final Action secondary;
    private boolean enableCardImageZoom = true;
    private BaseCard card;

    public GameCardView(@NonNull Context context) {
        this(context, null, 0);
    }

    public GameCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, null, null, null, null);
    }

    public GameCardView(@NonNull Context context, @Nullable BaseCard card, @Nullable Action primaryAction, @Nullable Action secondaryAction, @Nullable CardListener listener) {
        this(context, null, 0, card, primaryAction, secondaryAction, listener);
    }

    private GameCardView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @Nullable BaseCard card, @Nullable Action primary, @Nullable Action secondary, @Nullable CardListener listener) {
        super(context, attrs, defStyleAttr);
        this.card = card;
        this.primary = primary;
        this.secondary = secondary;
        this.listener = listener;

        LayoutInflater.from(getContext()).inflate(R.layout.pyx_card, this, true);
        mWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, WIDTH_DIP, getResources().getDisplayMetrics());
        setCardElevation((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));

        text = findViewById(R.id.pyxCard_text);

        watermark = findViewById(R.id.pyxCard_watermark);
        numPick = findViewById(R.id.pyxCard_numPick);
        numDraw = findViewById(R.id.pyxCard_numDraw);
        primaryAction = findViewById(R.id.pyxCard_primaryAction);
        secondaryAction = findViewById(R.id.pyxCard_secondaryAction);

        notText = findViewById(R.id.pyxCard_notText);
        loading = notText.findViewById(R.id.pyxCard_loading);
        image = notText.findViewById(R.id.pyxCard_image);
        unknown = notText.findViewById(R.id.pyxCard_unknown);

        init();
    }

    public void setSelectable(boolean selectable) {
        setupActions();
        setClickable(selectable);
        setFocusable(selectable);
    }

    public void setCardImageZoomEnabled(boolean enabled) {
        enableCardImageZoom = enabled;
        setupActions();
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
        primaryAction.setVisibility(GONE);
        secondaryAction.setVisibility(GONE);

        setCardBackgroundColor(getWhiteBackground());
    }

    private void setupAction(@Nullable Action action, @NonNull ImageButton button) {
        if (action != null) {
            button.setVisibility(VISIBLE);
            button.setOnClickListener(new CallActionListenerOnClick(action));

            switch (action) {
                case SELECT:
                    // Shouldn't be called
                    break;
                case DELETE:
                    button.setImageResource(R.drawable.baseline_delete_24);
                    break;
                case TOGGLE_STAR:
                    button.setImageResource(R.drawable.baseline_star_24);
                    break;
                case SELECT_IMG:
                    if (card.getImageUrl() == null) button.setVisibility(GONE);
                    else button.setImageResource(R.drawable.baseline_zoom_in_24);
                    break;
            }
        } else {
            button.setVisibility(GONE);
        }
    }

    @ColorInt
    private int getWhiteBackground() {
        if (CommonUtils.isNightModeOn(getContext(), false)) return Color.LTGRAY;
        else return Color.WHITE;
    }

    private void setupColors() {
        int foreground = card.black() ? Color.WHITE : Color.BLACK;
        int background = card.black() ? Color.BLACK : getWhiteBackground();

        if (card instanceof GameCard && ((GameCard) card).isWinner())
            setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        else
            setCardBackgroundColor(background);

        text.setTextColor(foreground);
        watermark.setTextColor(foreground);
        numPick.setTextColor(foreground);
        numDraw.setTextColor(foreground);
        ImageViewCompat.setImageTintList(unknown, ColorStateList.valueOf(foreground));
        ImageViewCompat.setImageTintList(primaryAction, ColorStateList.valueOf(foreground));
        ImageViewCompat.setImageTintList(secondaryAction, ColorStateList.valueOf(foreground));
    }

    private void setupImageCard() {
        text.setVisibility(GONE);
        numDraw.setVisibility(GONE);
        numPick.setVisibility(GONE);

        notText.setVisibility(VISIBLE);
        unknown.setVisibility(GONE);
        loading.setVisibility(VISIBLE);

        image.setVisibility(VISIBLE); // Must be so or Glide won't load
        Glide.with(getContext().getApplicationContext()).load(card).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException ex, Object model, Target<Drawable> target, boolean isFirstResource) {
                notText.setVisibility(GONE);
                text.setVisibility(VISIBLE);
                text.setTextSize(13);
                text.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
                text.setText(R.string.failedLoadingImage);
                Log.e(TAG, "Failed loading image.", ex);
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
        text.setHtml(card.textUnescaped());

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

    private void setupActions() {
        if (card instanceof UnknownCard)
            return;

        if (primary == Action.SELECT) {
            setOnClickListener(new CallActionListenerOnClick(Action.SELECT));

            setupAction(secondary, primaryAction);
            setupAction(null, secondaryAction);
        } else {
            setupAction(primary, primaryAction);
            setupAction(secondary, secondaryAction);
            if (enableCardImageZoom) checkRoomForSelectImage();
        }
    }

    private void init() {
        if (card == null) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);
        setupColors();

        if (card instanceof UnknownCard) {
            showUnknown();
        } else {
            setupActions();

            watermark.setText(card.watermark());

            if (card.getImageUrl() != null) setupImageCard();
            else setupTextCard();
        }
    }

    private void checkRoomForSelectImage() {
        if (card != null && card.getImageUrl() != null) {
            if (primary == null) setupAction(Action.SELECT_IMG, primaryAction);
            else if (secondary == null) setupAction(Action.SELECT_IMG, secondaryAction);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
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

    interface CardListener {
        void onCardAction(@NonNull Action action, @NonNull BaseCard card);
    }

    private class CallActionListenerOnClick implements View.OnClickListener {
        private final Action action;

        CallActionListenerOnClick(@NonNull Action action) {
            this.action = action;
        }

        @Override
        public void onClick(View v) {
            if (listener != null) listener.onCardAction(action, card);
        }
    }
}
