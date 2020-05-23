package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;

public final class WhiteCardsFragment extends FragmentWithDialog { // TODO

    @NonNull
    public static WhiteCardsFragment get(@NonNull Context context, @Nullable Integer id) {
        WhiteCardsFragment fragment = new WhiteCardsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.whiteCards));
        if (id != null) args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }
}
