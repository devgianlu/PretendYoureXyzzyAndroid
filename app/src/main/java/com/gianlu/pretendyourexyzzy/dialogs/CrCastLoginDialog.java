package com.gianlu.pretendyourexyzzy.dialogs;

import android.content.Context;
import android.os.Bundle;
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
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.LoadableContentView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.google.android.material.textfield.TextInputLayout;

public class CrCastLoginDialog extends DialogFragment {
    private static final String TAG = CrCastLoginDialog.class.getSimpleName();
    private LoginListener listener;

    @NonNull
    public static CrCastLoginDialog get() {
        return new CrCastLoginDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener)
            listener = (LoginListener) context;
        else if (getParentFragment() instanceof LoginListener)
            listener = (LoginListener) getParentFragment();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_cr_cast_login, container, false);
        LoadableContentView loadable = layout.findViewById(R.id.crCastLoginDialog_loadable);
        loadable.notLoading(false);
        loadable.setScrimColor(CommonUtils.resolveAttrAsColor(requireContext(), R.attr.colorBackgroundFloating));

        TextInputLayout username = layout.findViewById(R.id.crCastLoginDialog_username);
        TextInputLayout password = layout.findViewById(R.id.crCastLoginDialog_password);

        Button cancel = layout.findViewById(R.id.crCastLoginDialog_cancel);
        cancel.setOnClickListener(v -> dismissAllowingStateLoss());

        Button login = layout.findViewById(R.id.crCastLoginDialog_login);
        login.setOnClickListener(v -> {
            String userStr = CommonUtils.getText(username);
            String passStr = CommonUtils.getText(password);

            loadable.loading(true);
            setCancelable(false);
            CrCastApi.get().login(userStr, passStr)
                    .addOnSuccessListener(aVoid -> {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.signInSuccessful));
                        loadable.notLoading(false);
                        dismissAllowingStateLoss();

                        if (listener != null) listener.loggedInCrCast();
                    })
                    .addOnFailureListener(ex -> {
                        loadable.notLoading(true);
                        setCancelable(true);

                        if (ex instanceof CrCastApi.CrCastException) {
                            switch (((CrCastApi.CrCastException) ex).code) {
                                case NOT_AUTHORIZED:
                                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.wrongUsernameOrPassword));
                                    return;
                                case BANNED:
                                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.crCastBanned));
                                    return;
                            }
                        }

                        Log.e(TAG, "Failed signing in!", ex);
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSigningIn));
                    });
        });

        return layout;
    }

    public interface LoginListener {
        void loggedInCrCast();
    }
}
