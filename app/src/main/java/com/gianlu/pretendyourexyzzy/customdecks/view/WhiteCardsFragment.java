package com.gianlu.pretendyourexyzzy.customdecks.view;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.gianlu.pretendyourexyzzy.customdecks.AbsCardsFragment;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xyz.gianlu.pyxoverloaded.model.Card;

public final class WhiteCardsFragment extends AbsCardsFragment {
    private List<ContentCard> cards;

    @NotNull
    private static WhiteCardsFragment get(@NotNull Context context, @NotNull List<ContentCard> cards) {
        WhiteCardsFragment fragment = new WhiteCardsFragment();
        fragment.cards = cards;
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.whiteCards));
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    public static WhiteCardsFragment getWithOverloadedCards(@NotNull Context context, @NotNull List<Card> cards) {
        return get(context, ContentCard.fromOverloadedCards(cards));
    }

    @NotNull
    public static WhiteCardsFragment getWithBaseCards(@NotNull Context context, @NotNull List<? extends BaseCard> cards) {
        return get(context, ContentCard.fromBaseCards(cards));
    }

    @NonNull
    @Override
    protected List<? extends BaseCard> getCards() {
        return cards;
    }

    @Override
    protected boolean editable() {
        return false;
    }
}
