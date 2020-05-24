package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class GeneralInfoFragment extends FragmentWithDialog {
    private static final Pattern VALID_WATERMARK_PATTERN = Pattern.compile("[A-Z0-9]{5}");
    private TextInputLayout name;
    private TextInputLayout watermark;
    private TextInputLayout desc;
    private CustomDecksDatabase db;
    private CustomDeck deck;

    @NonNull
    public static GeneralInfoFragment get(@NonNull Context context, @Nullable Integer id) {
        GeneralInfoFragment fragment = new GeneralInfoFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        if (id != null) args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    public boolean save() {
        if (name == null || watermark == null || desc == null || getContext() == null)
            return false;

        String nameStr = CommonUtils.getText(name);
        if (nameStr.trim().isEmpty())
            return false;

        String watermarkStr = CommonUtils.getText(watermark);
        if (!VALID_WATERMARK_PATTERN.matcher(watermarkStr).matches())
            return false;

        String descStr = CommonUtils.getText(desc);

        CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());
        if (deck == null) {
            deck = db.putDeckInfo(nameStr, watermarkStr, descStr);
            return deck != null;
        } else {
            db.updateDeckInfo(deck.id, nameStr, watermarkStr, descStr);
            return true;
        }
    }

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_edit_custom_deck_info, container, false);
        name = layout.findViewById(R.id.editCustomDeckInfo_name);
        CommonUtils.getEditText(name).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString().trim();
                if (str.isEmpty()) {
                    name.setErrorEnabled(true);
                    name.setError(getString(R.string.emptyDeckName));
                } else if ((deck == null || !deck.name.equals(str)) && db != null && !db.isNameUnique(str)) {
                    name.setErrorEnabled(true);
                    name.setError(getString(R.string.customDeckNameNotUnique));
                } else {
                    name.setErrorEnabled(false);
                }
            }
        });
        watermark = layout.findViewById(R.id.editCustomDeckInfo_watermark);
        CommonUtils.getEditText(watermark).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (VALID_WATERMARK_PATTERN.matcher(str).matches()) {
                    watermark.setErrorEnabled(false);
                } else {
                    watermark.setErrorEnabled(true);
                    watermark.setError(getString(R.string.invalidWatermark));
                }
            }
        });
        desc = layout.findViewById(R.id.editCustomDeckInfo_desc);

        db = CustomDecksDatabase.get(requireContext());

        int id = requireArguments().getInt("id", -1);
        if (id == -1) deck = null;
        else deck = db.getDeck(id);

        if (deck != null) {
            CommonUtils.setText(name, deck.name);
            CommonUtils.setText(watermark, deck.watermark);
            CommonUtils.setText(desc, deck.description);
        }

        return layout;
    }
}
