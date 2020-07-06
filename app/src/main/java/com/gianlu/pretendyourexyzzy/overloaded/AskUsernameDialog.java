package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.LoadableContentView;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.auth.UserInfo;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.BooleanCallback;

public final class AskUsernameDialog extends DialogFragment {
    private static final String TAG = AskUsernameDialog.class.getSimpleName();
    private Listener listener;

    @NonNull
    public static AskUsernameDialog get() {
        return new AskUsernameDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
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

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_ask_overloaded_username, container, false);
        LoadableContentView loadable = layout.findViewById(R.id.askUsernameDialog_loadable);
        loadable.notLoading(false);
        loadable.setScrimColor(CommonUtils.resolveAttrAsColor(requireContext(), R.attr.colorBackgroundFloating));

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

                        if (OverloadedUtils.checkUsernameValid(username)) {
                            OverloadedApi.get().isUsernameUnique(username, getActivity(), new BooleanCallback() {
                                @Override
                                public void onResult(boolean result) {
                                    if (!result)
                                        input.setError(getString(R.string.overloaded_usernameAlreadyInUse));
                                    else
                                        input.setErrorEnabled(false);
                                }

                                @Override
                                public void onFailed(@NonNull Exception ex) {
                                    Log.w(TAG, "Failed checking username unique.", ex);
                                }
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
            if (OverloadedUtils.checkUsernameValid(username)) {
                loadable.loading(true);
                OverloadedApi.get().isUsernameUnique(username, getActivity(), new BooleanCallback() {
                    @Override
                    public void onResult(boolean result) {
                        if (result) {
                            if (listener != null) listener.onUsernameSelected(username);
                            dismissAllowingStateLoss();
                        } else {
                            input.setError(getString(R.string.overloaded_usernameAlreadyInUse));
                            loadable.notLoading(true);
                        }
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        input.setError(getString(R.string.failedCheckingUsername));
                        loadable.notLoading(true);
                        Log.e(TAG, "Failed checking username unique.", ex);
                    }
                });
            } else {
                input.setError(getString(R.string.overloaded_invalidUsername));
            }
        });

        return layout;
    }

    public interface Listener {
        void onUsernameSelected(@NonNull String username);
    }
}
