package com.gianlu.pretendyourexyzzy.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.databinding.DialogCrCastLoginBinding;

public final class CrCastLoginDialog extends DialogFragment {
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

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogCrCastLoginBinding binding = DialogCrCastLoginBinding.inflate(inflater, container, false);
        binding.crCastLoginDialogCancel.setOnClickListener(v -> dismissAllowingStateLoss());

        binding.crCastLoginDialogLoading.hideShimmer();
        binding.crCastLoginDialogLogin.setOnClickListener(v -> {
            String userStr = CommonUtils.getText(binding.crCastLoginDialogUsername);
            String passStr = CommonUtils.getText(binding.crCastLoginDialogPassword);

            binding.crCastLoginDialogLoading.showShimmer(true);
            binding.crCastLoginDialogCancel.setEnabled(false);
            binding.crCastLoginDialogLogin.setEnabled(false);

            setCancelable(false);
            CrCastApi.get().login(userStr, passStr)
                    .addOnSuccessListener(aVoid -> {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.signInSuccessful));
                        dismissAllowingStateLoss();

                        if (listener != null) listener.loggedInCrCast();
                    })
                    .addOnFailureListener(ex -> {
                        binding.crCastLoginDialogLoading.hideShimmer();
                        binding.crCastLoginDialogCancel.setEnabled(true);
                        binding.crCastLoginDialogLogin.setEnabled(true);
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

        return binding.getRoot();
    }

    public interface LoginListener {
        void loggedInCrCast();
    }
}
