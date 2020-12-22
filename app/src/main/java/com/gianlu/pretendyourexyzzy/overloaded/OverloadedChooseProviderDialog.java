package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.main.NewProfileFragment;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;
import com.google.firebase.auth.AuthCredential;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public final class OverloadedChooseProviderDialog extends DialogFragment {
    private static final int RC_SIGN_IN = 88;
    private static final String TAG = OverloadedChooseProviderDialog.class.getSimpleName();
    private final OverloadedSignInHelper signInHelper = new OverloadedSignInHelper();

    @NonNull
    private static OverloadedChooseProviderDialog getInstance(boolean link, @NonNull List<String> providers) {
        if (providers.isEmpty()) throw new IllegalArgumentException();

        OverloadedChooseProviderDialog dialog = new OverloadedChooseProviderDialog();
        Bundle args = new Bundle();
        args.putBoolean("link", link);
        args.putCharSequenceArray("providers", providers.toArray(new String[0]));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    public static OverloadedChooseProviderDialog getLinkInstance(@NonNull List<String> providers) {
        return getInstance(true, providers);
    }

    @NonNull
    public static OverloadedChooseProviderDialog getSignInInstance() {
        return getInstance(false, OverloadedSignInHelper.providerIds());
    }

    @Override
    public void onResume() {
        super.onResume();

        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null)
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @NonNull
    private View createProviderItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull SignInProvider provider) {
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_overloaded_sign_in_provider, container, false);
        ((ImageView) item.findViewById(R.id.overloadedSignInProvider_icon)).setImageResource(provider.iconRes);
        ((TextView) item.findViewById(R.id.overloadedSignInProvider_name)).setText(provider.nameRes);
        item.setOnClickListener(v -> {
            if (getActivity() == null) return;
            startActivityForResult(signInHelper.startFlow(requireActivity(), provider), RC_SIGN_IN);
        });
        return item;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_SIGN_IN && data != null && requireArguments().getBoolean("link", false)) {
            AuthCredential credential = signInHelper.extractCredential(data);
            if (credential == null) {
                Log.w(TAG, "Couldn't extract credentials: " + data);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSigningIn));
                dismissAllowingStateLoss();
                return;
            }

            OverloadedApi.get().link(credential)
                    .addOnSuccessListener(aVoid -> {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.accountLinked));

                        if (getParentFragment() instanceof NewSettingsFragment.PrefsChildFragment)
                            ((NewSettingsFragment.PrefsChildFragment) getParentFragment()).rebuildPreferences();
                        else if (getParentFragment() instanceof NewProfileFragment)
                            ((NewProfileFragment) getParentFragment()).refreshOverloaded(true);

                        dismissAllowingStateLoss();
                    })
                    .addOnFailureListener(ex -> {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLinkingAccount));
                        Log.e(TAG, "Failed linking account.", ex);
                        dismissAllowingStateLoss();
                    });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_overloaded_choose_provider, container, false);

        TextView title = layout.findViewById(R.id.overloadedSignInDialog_title);
        TextView desc = layout.findViewById(R.id.overloadedSignInDialog_desc);
        if (requireArguments().getBoolean("link", false)) {
            title.setText(R.string.overloaded_linkAccount);
            desc.setText(R.string.overloaded_linkAccount_desc);
        } else {
            title.setText(R.string.overloaded_requiresSignIn);
            desc.setText(R.string.overloaded_requiresSignIn_desc);
        }

        Button cancel = layout.findViewById(R.id.overloadedSignInDialog_cancel);
        cancel.setOnClickListener(v -> dismissAllowingStateLoss());

        LinearLayout providersLayout = layout.findViewById(R.id.overloadedSignInDialog_providers);
        providersLayout.removeAllViews();

        CharSequence[] providers = requireArguments().getCharSequenceArray("providers");
        if (providers == null) throw new IllegalStateException();

        for (SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
            if (CommonUtils.contains(providers, provider.id))
                providersLayout.addView(createProviderItem(inflater, providersLayout, provider));
        }

        return layout;
    }
}
