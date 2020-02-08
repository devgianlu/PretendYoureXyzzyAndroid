package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;

public class OverloadedFragment extends FragmentWithDialog {

    @NonNull
    public static OverloadedFragment getInstance() {
        return new OverloadedFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_overloaded, container, false);

        // TODO

        return layout;
    }
}
