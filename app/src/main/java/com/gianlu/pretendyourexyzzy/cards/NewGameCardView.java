package com.gianlu.pretendyourexyzzy.cards;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.UnknownCard;
import com.gianlu.pretendyourexyzzy.databinding.ViewNewCardBiggerBinding;
import com.gianlu.pretendyourexyzzy.databinding.ViewNewCardBinding;

import org.jetbrains.annotations.NotNull;

public final class NewGameCardView extends CardView {
    private static final String TAG = NewGameCardView.class.getSimpleName();
    private final ViewNewCardBinding binding;
    private BaseCard card;

    public NewGameCardView(@NonNull Context context, boolean bigger) {
        this(context, null, 0, bigger);
    }

    public NewGameCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, false);
    }

    public NewGameCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, false);
    }

    private NewGameCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, boolean bigger) {
        super(context, attrs, defStyleAttr);
        if (bigger) ViewNewCardBiggerBinding.inflate(LayoutInflater.from(context), this);
        else ViewNewCardBinding.inflate(LayoutInflater.from(context), this);
        binding = ViewNewCardBinding.bind(this);

        int dp4 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        setElevation(dp4);
        setRadius(dp4 * 2);

        setForeground(CommonUtils.resolveAttrAsDrawable(context, android.R.attr.selectableItemBackground));

        unsetLeftAction();
        unsetRightAction();
    }

    public void setLeftAction(@DrawableRes int iconRes, @Nullable OnClickListener listener) {
        if (card instanceof UnknownCard)
            return;

        binding.cardItemActionLeft.setVisibility(VISIBLE);
        binding.cardItemActionLeft.setImageResource(iconRes);
        binding.cardItemActionLeft.setOnClickListener(listener);
    }

    public void unsetLeftAction() {
        binding.cardItemActionLeft.setVisibility(GONE);
        binding.cardItemActionLeft.setOnClickListener(null);
    }

    public void setRightAction(@DrawableRes int iconRes, @Nullable OnClickListener listener) {
        if (card instanceof UnknownCard)
            return;

        binding.cardItemActionRight.setVisibility(VISIBLE);
        binding.cardItemActionRight.setImageResource(iconRes);
        binding.cardItemActionRight.setOnClickListener(listener);
    }

    public void unsetRightAction() {
        binding.cardItemActionRight.setVisibility(GONE);
        binding.cardItemActionRight.setOnClickListener(null);
    }

    public void setCard(@NotNull BaseCard card) {
        this.card = card;

        if (card instanceof GameCard && ((GameCard) card).isWinner())
            setType(Type.WINNER);
        else if (card instanceof UnknownCard)
            setType(Type.UNKNOWN);
        else
            setType(card.black() ? Type.BLACK : Type.WHITE);
    }

    private void setType(@NotNull Type type) {
        if (card == null) throw new IllegalStateException();
        if (type == Type.UNKNOWN) {
            setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            binding.cardItemPick.setVisibility(GONE);
            binding.cardItemText.setVisibility(GONE);
            binding.cardItemWatermark.setVisibility(GONE);
            binding.cardItemActionRight.setVisibility(GONE);
            binding.cardItemActionLeft.setVisibility(GONE);

            binding.cardItemImage.setVisibility(VISIBLE);
            binding.cardItemImage.setImageResource(R.drawable.baseline_question_mark_192);
            return;
        }

        binding.cardItemActionRight.setVisibility(binding.cardItemActionRight.hasOnClickListeners() ? VISIBLE : GONE);
        binding.cardItemActionLeft.setVisibility(binding.cardItemActionLeft.hasOnClickListeners() ? VISIBLE : GONE);

        binding.cardItemWatermark.setText(card.watermark());

        if (card.getImageUrl() != null) {
            binding.cardItemText.setVisibility(GONE);
            binding.cardItemImage.setVisibility(VISIBLE);
            Glide.with(getContext().getApplicationContext()).load(card).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException ex, Object model, Target<Drawable> target, boolean isFirstResource) {
                    binding.cardItemText.setVisibility(VISIBLE);
                    binding.cardItemImage.setVisibility(GONE);
                    binding.cardItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
                    binding.cardItemText.setText(R.string.failedLoadingImage);
                    Log.e(TAG, "Failed loading image.", ex);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    binding.cardItemText.setVisibility(GONE);
                    binding.cardItemImage.setVisibility(VISIBLE);
                    return false;
                }
            }).into(binding.cardItemImage);
        } else {
            binding.cardItemImage.setVisibility(GONE);
            binding.cardItemText.setVisibility(VISIBLE);
            binding.cardItemText.setText(card.textUnescaped());
        }

        switch (type) {
            case WINNER:
            case WHITE:
                binding.cardItemPick.setVisibility(GONE);

                if (type == Type.WINNER)
                    setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.appColor_500)));
                else
                    setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));

                binding.cardItemText.setTextColor(Color.BLACK);

                binding.cardItemActionRight.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                binding.cardItemActionLeft.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                binding.cardItemActionRight.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(242, 242, 242)));
                binding.cardItemActionLeft.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(242, 242, 242)));
                break;
            case BLACK:
                binding.cardItemPick.setVisibility(VISIBLE);
                binding.cardItemPick.setHtml(R.string.numPick, card.numPick());

                setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                binding.cardItemText.setTextColor(Color.WHITE);

                binding.cardItemActionRight.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                binding.cardItemActionLeft.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                binding.cardItemActionRight.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(74, 74, 74)));
                binding.cardItemActionLeft.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(74, 74, 74)));
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setSelectable(boolean selectable) {
        setClickable(selectable);
    }

    public enum Type {
        WHITE, BLACK, WINNER, UNKNOWN
    }
}
