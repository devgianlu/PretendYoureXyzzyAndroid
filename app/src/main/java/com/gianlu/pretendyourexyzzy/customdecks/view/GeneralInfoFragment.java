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
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.model.UserProfile;

public final class GeneralInfoFragment extends FragmentWithDialog {

    @NotNull
    public static GeneralInfoFragment get(@NotNull Context context, @NotNull UserProfile.CustomDeckWithCards deck) {
        GeneralInfoFragment fragment = new GeneralInfoFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        args.putBoolean("crCast", false);
        args.putString("name", deck.name);
        args.putString("watermark", deck.watermark);
        args.putString("desc", deck.desc);
        args.putBoolean("collaborator", deck.collaborator);
        fragment.setArguments(args);
        return fragment;
    }

    @NotNull
    public static GeneralInfoFragment get(@NotNull Context context, @NotNull CrCastDeck deck) {
        GeneralInfoFragment fragment = new GeneralInfoFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        args.putBoolean("crCast", true);
        args.putString("name", deck.name);
        args.putString("watermark", deck.watermark);
        args.putString("desc", deck.desc);
        args.putString("lang", deck.lang);
        args.putBoolean("private", deck.privateDeck);
        args.putSerializable("state", deck.state);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public String getWatermark() {
        return requireArguments().getString("watermark", "");
    }

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_view_custom_deck_info, container, false);
        Bundle args = requireArguments();

        TextView name = layout.findViewById(R.id.viewCustomDeck_name);
        name.setText(args.getString("name"));

        TextView watermark = layout.findViewById(R.id.viewCustomDeck_watermark);
        watermark.setText(args.getString("watermark"));

        String descStr = args.getString("desc");
        if (descStr == null || descStr.isEmpty()) {
            layout.findViewById(R.id.viewCustomDeck_desc).setVisibility(View.GONE);
            layout.findViewById(R.id.viewCustomDeck_descLabel).setVisibility(View.GONE);
        } else {
            TextView desc = layout.findViewById(R.id.viewCustomDeck_desc);
            desc.setText(descStr);
        }

        LinearLayout collaborating = layout.findViewById(R.id.viewCustomDeck_collaborating);
        LinearLayout crCast = layout.findViewById(R.id.viewCustomDeck_crCast);
        if (args.getBoolean("crCast", false)) {
            crCast.setVisibility(View.VISIBLE);
            collaborating.setVisibility(View.GONE);

            TextView state = crCast.findViewById(R.id.viewCustomDeck_state);
            state.setText(((CrCastApi.State) args.getSerializable("state")).toFormal());

            TextView privateDeck = crCast.findViewById(R.id.viewCustomDeck_private);
            privateDeck.setText(args.getBoolean("private") ? R.string.yes : R.string.no);

            TextView lang = crCast.findViewById(R.id.viewCustomDeck_lang);
            lang.setText(args.getString("lang"));
        } else {
            crCast.setVisibility(View.GONE);
            collaborating.setVisibility(View.VISIBLE);

            TextView canCollaborate = collaborating.findViewById(R.id.viewCustomDeck_canCollaborate);
            canCollaborate.setText(args.getBoolean("collaborator", false) ? R.string.yes : R.string.no);
        }

        return layout;
    }
}
