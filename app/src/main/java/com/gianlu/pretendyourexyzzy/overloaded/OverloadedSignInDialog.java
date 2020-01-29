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

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;

import org.jetbrains.annotations.NotNull;

public final class OverloadedSignInDialog extends DialogFragment {
    private Listener listener;

    @NonNull
    public static OverloadedSignInDialog getInstance() {
        return new OverloadedSignInDialog();
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
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_overloaded_sign_in, container, false);

        Button cancel = layout.findViewById(R.id.overloadedSignInDialog_cancel);
        cancel.setOnClickListener(v -> dismissAllowingStateLoss());

        LinearLayout providersLayout = layout.findViewById(R.id.overloadedSignInDialog_providers);
        providersLayout.removeAllViews();

        for (SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS)
            providersLayout.addView(createProviderItem(inflater, providersLayout, provider));

        return layout;
    }

    public interface Listener {
        void onSelectedSignInProvider(@NonNull SignInProvider provider);
    }
}
