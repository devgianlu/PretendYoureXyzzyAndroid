package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.AbsCardsFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class WhiteCardsFragment extends AbsCardsFragment {

    @NonNull
    public static WhiteCardsFragment get(@NonNull Context context, @Nullable Integer id) {
        WhiteCardsFragment fragment = new WhiteCardsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.whiteCards));
        if (id != null) args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    @Override
    protected List<? extends BaseCard> getCards() {
        if (id == null) return new ArrayList<>(0);
        else return db.getWhiteCards(id);
    }

    @Override
    protected boolean editable() {
        return true;
    }
}
