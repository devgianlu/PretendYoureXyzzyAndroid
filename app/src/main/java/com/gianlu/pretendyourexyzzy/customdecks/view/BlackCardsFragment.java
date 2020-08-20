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

public final class BlackCardsFragment extends AbsCardsFragment {
    private List<ContentCard> cards;

    @NotNull
    private static BlackCardsFragment get(@NotNull Context context, boolean collaborate, @NotNull List<ContentCard> cards) {
        BlackCardsFragment fragment = new BlackCardsFragment();
        fragment.cards = cards;
        Bundle args = new Bundle();
        args.putInt("titleWithCountRes", R.string.blackCardsWithCount);
        args.putString("title", context.getString(R.string.blackCards));
        args.putBoolean("collaborate", collaborate);
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    public static BlackCardsFragment getWithOverloadedCards(@NotNull Context context, boolean collaborate, @NotNull List<Card> cards) {
        return get(context, collaborate, ContentCard.fromOverloadedCards(cards));
    }

    @NotNull
    public static BlackCardsFragment getWithBaseCards(@NotNull Context context, boolean collaborate, @NotNull List<? extends BaseCard> cards) {
        return get(context, collaborate, ContentCard.fromBaseCards(cards));
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

    @Override
    protected boolean canCollaborate() {
        return requireArguments().getBoolean("collaborate", false);
    }
}
