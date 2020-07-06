package com.gianlu.pretendyourexyzzy.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class OverloadedUserDialog extends DialogFragment {
    @NonNull
    public static OverloadedUserDialog get(@NonNull String username) {
        OverloadedUserDialog dialog = new OverloadedUserDialog();
        Bundle args = new Bundle();
        args.putString("username", username);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
