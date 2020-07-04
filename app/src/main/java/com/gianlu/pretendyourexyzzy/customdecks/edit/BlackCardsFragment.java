package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BlackCardsFragment extends AbsCardsFragment {

    @NonNull
    public static BlackCardsFragment get(@NonNull Context context, @Nullable Integer id) {
        BlackCardsFragment fragment = new BlackCardsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.blackCards));
        if (id != null) args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    @Override
    protected List<? extends BaseCard> getCards(int id) {
        return db.loadBlackCards(id);
    }
}
