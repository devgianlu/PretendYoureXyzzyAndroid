package com.gianlu.pretendyourexyzzy.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.R;

public class InfoFragment extends Fragment { // TODO

    public static InfoFragment getInstance(Context context, String code) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putString("code", code);
        args.putString("title", context.getString(R.string.info));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
