package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
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
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OverloadedChooseProviderDialog extends DialogFragment {
    private Listener listener;

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (Listener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
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
            if (listener != null) listener.onSelectedSignInProvider(provider);
            dismissAllowingStateLoss();
        });
        return item;
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

    public interface Listener {
        void onSelectedSignInProvider(@NonNull SignInProvider provider);
    }
}
