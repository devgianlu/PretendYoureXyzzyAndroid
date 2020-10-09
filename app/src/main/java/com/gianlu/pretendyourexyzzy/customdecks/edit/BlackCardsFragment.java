package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.AbsCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksHandler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BlackCardsFragment extends AbsCardsFragment {

    @NonNull
    public static BlackCardsFragment get(@NonNull Context context) {
        BlackCardsFragment fragment = new BlackCardsFragment();
        Bundle args = new Bundle();
        args.putInt("titleWithCountRes", R.string.blackCardsWithCount);
        args.putString("title", context.getString(R.string.blackCards));
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    @Override
    protected List<? extends BaseCard> getCards() {
        if (handler == null || !(handler instanceof CustomDecksHandler)) return new ArrayList<>(0);
        else return db.getBlackCards(((CustomDecksHandler) handler).id);
    }

    @Override
    protected boolean editable() {
        return true;
    }

    @Override
    protected boolean canCollaborate() {
        return true;
    }
}
