package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.auth.UserInfo;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

public final class AskUsernameDialog extends DialogFragment {

    @NonNull
    public static AskUsernameDialog get() {
        return new AskUsernameDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCancelable(false);
        return dialog;
    }

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_ask_overloaded_username, container, false);
        Button done = layout.findViewById(R.id.askUsernameDialog_done);
        TextInputLayout input = layout.findViewById(R.id.askUsernameDialog_input);
        CommonUtils.clearErrorOnEdit(input);
        CommonUtils.getEditText(input).addTextChangedListener(new TextWatcher() {
            private final Timer timer = new Timer();
            private TimerTask lastTask;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (lastTask != null) lastTask.cancel();
                timer.schedule(lastTask = new TimerTask() {
                    @Override
                    public void run() {
                        String username = CommonUtils.getText(input);
                        if (username.trim().isEmpty()) return;

                        if (OverloadedApi.checkUsernameValid(username)) {
                            OverloadedApi.isUsernameUnique(username).addOnCompleteListener(task -> {
                                if (task.getResult() == null || !task.getResult())
                                    new Handler(Looper.getMainLooper()).post(() -> input.setError(getString(R.string.overloaded_usernameAlreadyInUse)));
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> input.setError(getString(R.string.overloaded_invalidUsername)));
                        }
                    }
                }, 500);
            }
        });

        UserInfo playGamesInfo;
        if ((playGamesInfo = OverloadedApi.get().getProviderUserInfo(PlayGamesAuthProvider.PROVIDER_ID)) != null)
            CommonUtils.setText(input, playGamesInfo.getDisplayName());

        done.setOnClickListener(v -> {
            String username = CommonUtils.getText(input);
            if (OverloadedApi.checkUsernameValid(username)) {
                // TODO: Trigger loading state
                OverloadedApi.isUsernameUnique(username).addOnCompleteListener(task -> {
                    if (task.getResult() != null && task.getResult()) {
                        OverloadedApi.get().setUsername(username, new OverloadedApi.SuccessfulCallback() {
                            @Override
                            public void onSuccessful() {
                                DialogUtils.showToast(getActivity(), Toaster.build().message(R.string.usernameSetSuccessfully));
                                dismissAllowingStateLoss();
                            }

                            @Override
                            public void onFailed(@NonNull Exception ex) {
                                input.setError(getString(R.string.failedSettingUsername));
                                Logging.log(ex);
                            }
                        });
                    } else {
                        input.setError(getString(R.string.overloaded_usernameAlreadyInUse));
                    }
                });
            } else {
                input.setError(getString(R.string.overloaded_invalidUsername));
            }
        });

        return layout;
    }
}
