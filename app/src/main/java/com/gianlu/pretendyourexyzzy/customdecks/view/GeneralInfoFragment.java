package com.gianlu.pretendyourexyzzy.customdecks.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;

import org.jetbrains.annotations.NotNull;

public final class GeneralInfoFragment extends FragmentWithDialog {

    @NotNull
    public static GeneralInfoFragment get(@NotNull Context context, @NotNull String name, @NotNull String watermark, @NotNull String description) {
        GeneralInfoFragment fragment = new GeneralInfoFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        args.putString("name", name);
        args.putString("watermark", watermark);
        args.putString("desc", description);
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_view_custom_deck_info, container, false);
        TextView name = layout.findViewById(R.id.viewCustomDeck_name);
        name.setText(requireArguments().getString("name"));

        TextView desc = layout.findViewById(R.id.viewCustomDeck_desc);
        desc.setText(requireArguments().getString("desc"));

        TextView watermark = layout.findViewById(R.id.viewCustomDeck_watermark);
        watermark.setText(requireArguments().getString("watermark"));

        return layout;
    }
}
