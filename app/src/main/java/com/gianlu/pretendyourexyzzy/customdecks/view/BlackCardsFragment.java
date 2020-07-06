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
    public static BlackCardsFragment get(@NotNull Context context, @NotNull List<Card> cards) {
        BlackCardsFragment fragment = new BlackCardsFragment();
        fragment.cards = ContentCard.from(cards);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.blackCards));
        fragment.setArguments(args);
        return fragment;
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
