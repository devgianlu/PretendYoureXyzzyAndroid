package com.gianlu.pretendyourexyzzy.cards;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.databinding.ViewNewCardBinding;

import org.jetbrains.annotations.NotNull;

public final class NewGameCardView extends CardView {
    private static final int CARD_HEIGHT_DIP = 220;
    private static final int CARD_WIDTH_DIP = 160;
    private final ViewNewCardBinding binding;
    private BaseCard card;

    public NewGameCardView(@NonNull Context context) {
        this(context, null, 0);
    }

    public NewGameCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NewGameCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = ViewNewCardBinding.inflate(LayoutInflater.from(context), this);

        int dp4 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
        setElevation(dp4);
        setRadius(dp4 * 2);

        setForeground(CommonUtils.resolveAttrAsDrawable(context, android.R.attr.selectableItemBackground));
    }

    public void setCard(@NotNull BaseCard card) {
        this.card = card;
        setType(card.black() ? Type.BLACK : Type.WHITE);
    }

    private void setType(@NotNull Type type) {
        if (card == null) throw new IllegalStateException();

        binding.cardItemText.setText(card.textUnescaped());
        binding.cardItemWatermark.setText(card.watermark());

        switch (type) {
            case WHITE:
                binding.cardItemPick.setVisibility(GONE);

                setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                binding.cardItemText.setTextColor(Color.BLACK);
                break;
            case BLACK:
                binding.cardItemPick.setVisibility(VISIBLE);
                binding.cardItemPick.setHtml(R.string.numPick, card.numPick());

                setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                binding.cardItemText.setTextColor(Color.WHITE);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CARD_WIDTH_DIP, getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CARD_HEIGHT_DIP, getResources().getDisplayMetrics());
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public enum Type {
        WHITE, BLACK
    }
}
