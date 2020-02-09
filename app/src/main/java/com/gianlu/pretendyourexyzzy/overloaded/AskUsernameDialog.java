package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.pretendyourexyzzy.R;
import com.google.android.material.textfield.TextInputLayout;

public final class AskUsernameDialog extends DialogFragment {
    private Listener listener;

    @NonNull
    public static AskUsernameDialog get() {
        return new AskUsernameDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (Listener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_ask_overloaded_username, container, false);

        TextInputLayout input = layout.findViewById(R.id.askUsernameDialog_input);

        // TODO: Layout !!

        return layout;
    }

    public interface Listener {
        void onUsernameChosen(@NonNull String username);
    }
}
