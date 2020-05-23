package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;

public final class BlackCardsFragment extends FragmentWithDialog { // TODO

    @NonNull
    public static BlackCardsFragment get(@NonNull Context context, @Nullable Integer id) {
        BlackCardsFragment fragment = new BlackCardsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.blackCards));
        if (id != null) args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }
}
