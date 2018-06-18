package com.gianlu.pretendyourexyzzy.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Deck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class EditGameOptionsDialog extends DialogFragment {
    private TextInputLayout scoreLimit;
    private TextInputLayout playerLimit;
    private TextInputLayout spectatorLimit;
    private Spinner timerMultiplier;
    private TextInputLayout blankCards;
    private TextInputLayout password;
    private LinearLayout decks;
    private LinearLayout layout;
    private int gid;
    private ApplyOptions listener;
    private TextView decksTitle;

    @NonNull
    public static EditGameOptionsDialog get(int gid, Game.Options options) {
        EditGameOptionsDialog dialog = new EditGameOptionsDialog();
        Bundle args = new Bundle();
        args.putInt("gid", gid);
        args.putSerializable("options", options);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ApplyOptions)
            listener = (ApplyOptions) context;
    }

    private void done() {
        scoreLimit.setErrorEnabled(false);
        playerLimit.setErrorEnabled(false);
        spectatorLimit.setErrorEnabled(false);
        blankCards.setErrorEnabled(false);
        password.setErrorEnabled(false);

        Game.Options newOptions;
        try {
            newOptions = Game.Options.validateAndCreate(timerMultiplier.getSelectedItem().toString(), CommonUtils.getText(spectatorLimit), CommonUtils.getText(playerLimit), CommonUtils.getText(scoreLimit), CommonUtils.getText(blankCards), decks, CommonUtils.getText(password));
        } catch (Game.Options.InvalidFieldException ex) {
            View view = layout.findViewById(ex.fieldId);
            if (view != null && view instanceof TextInputLayout) {
                if (ex.throwMessage == R.string.outOfRange)
                    ((TextInputLayout) view).setError(getString(R.string.outOfRange, ex.min, ex.max));
                else
                    ((TextInputLayout) view).setError(getString(ex.throwMessage));
            }

            return;
        }

        dismissAllowingStateLoss();
        if (listener != null) listener.changeGameOptions(gid, newOptions);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.TitledDialog);
        dialog.setTitle(R.string.editGameOptions);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.dialog_edit_game_options, container, false);

        Bundle args = getArguments();
        Game.Options options;
        if (args == null || (options = (Game.Options) args.getSerializable("options")) == null
                || (gid = args.getInt("gid", -1)) == -1 || getContext() == null) {
            dismissAllowingStateLoss();
            return layout;
        }

        RegisteredPyx pyx;
        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
            dismissAllowingStateLoss();
            return layout;
        }

        scoreLimit = layout.findViewById(R.id.editGameOptions_scoreLimit);
        CommonUtils.setText(scoreLimit, String.valueOf(options.scoreLimit));

        playerLimit = layout.findViewById(R.id.editGameOptions_playerLimit);
        CommonUtils.setText(playerLimit, String.valueOf(options.playersLimit));

        spectatorLimit = layout.findViewById(R.id.editGameOptions_spectatorLimit);
        CommonUtils.setText(spectatorLimit, String.valueOf(options.spectatorsLimit));

        timerMultiplier = layout.findViewById(R.id.editGameOptions_timerMultiplier);
        timerMultiplier.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Game.Options.VALID_TIMER_MULTIPLIERS));
        timerMultiplier.setSelection(Game.Options.timerMultiplierIndex(options.timerMultiplier));

        blankCards = layout.findViewById(R.id.editGameOptions_blankCards);
        CommonUtils.setText(blankCards, String.valueOf(options.blanksLimit));

        password = layout.findViewById(R.id.editGameOptions_password);
        CommonUtils.setText(password, options.password);

        decksTitle = layout.findViewById(R.id.editGameOptions_decksTitle);

        decks = layout.findViewById(R.id.editGameOptions_decks);
        decks.removeAllViews();
        for (Deck set : pyx.firstLoad().decks) {
            CheckBox item = new CheckBox(getContext());
            item.setTag(set);
            item.setText(set.name);
            item.setChecked(options.cardSets.contains(set.id));
            item.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    updateDecksCount();
                }
            });
            decks.addView(item);
        }

        updateDecksCount();

        Button cancel = layout.findViewById(R.id.editGameOptions_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        Button apply = layout.findViewById(R.id.editGameOptions_apply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });

        return layout;
    }

    private void updateDecksCount() {
        int count = 0;
        int black = 0;
        int white = 0;
        for (int i = 0; i < decks.getChildCount(); i++) {
            CheckBox view = (CheckBox) decks.getChildAt(i);
            if (view.isChecked()) {
                Deck deck = (Deck) view.getTag();
                count++;
                black += deck.blackCards;
                white += deck.whiteCards;
            }
        }

        decksTitle.setText(String.format("%s (%s)", getString(R.string.cardSetsLabel), Utils.buildDeckCountString(count, black, white)));
    }

    public interface ApplyOptions {
        void changeGameOptions(int gid, @NonNull Game.Options options);
    }
}
