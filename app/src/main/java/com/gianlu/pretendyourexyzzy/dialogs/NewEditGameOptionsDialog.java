package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.CahConfig;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.databinding.DialogNewEditGameOptionsBinding;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;

public class NewEditGameOptionsDialog extends DialogFragment {
    private static final String TAG = NewEditGameOptionsDialog.class.getSimpleName();
    private DialogNewEditGameOptionsBinding binding;
    private Game.Options options;
    private RegisteredPyx pyx;
    private int gid;

    @NotNull
    public static NewEditGameOptionsDialog get(int gid, @NotNull Game.Options options) {
        NewEditGameOptionsDialog dialog = new NewEditGameOptionsDialog();
        Bundle args = new Bundle();
        args.putInt("gid", gid);
        args.putSerializable("options", options);
        dialog.setArguments(args);
        return dialog;
    }

    private static int parseIntOrThrow(String val, @IdRes int fieldId) throws InvalidFieldException {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException ex) {
            throw new InvalidFieldException(fieldId, R.string.invalidNumber);
        }
    }

    private static void checkMaxMin(int val, int min, int max, @IdRes int fieldId) throws InvalidFieldException {
        if (val < min || val > max)
            throw new InvalidFieldException(fieldId, min, max);
    }

    @NonNull
    @Contract("_, _, _, _, _, _, _, _, _ -> new")
    private static Game.Options validateAndCreate(@NonNull CahConfig config, @NonNull Pyx.Server.Params params, String timerMultiplier, String spectatorsLimit,
                                                  String playersLimit, String scoreLimit, String blanksLimit, LinearLayout cardSets,
                                                  String password) throws InvalidFieldException {
        if (!CommonUtils.contains(Game.Options.VALID_TIMER_MULTIPLIERS, timerMultiplier))
            throw new InvalidFieldException(R.id.editGameOptions_timerMultiplier, R.string.invalidTimerMultiplier);

        int vL = parseIntOrThrow(spectatorsLimit, R.id.editGameOptions_spectatorLimit);
        checkMaxMin(vL, params.spectatorsMin, params.spectatorsMax, R.id.editGameOptions_spectatorLimit);

        int pL = parseIntOrThrow(playersLimit, R.id.editGameOptions_playerLimit);
        checkMaxMin(pL, params.playersMin, params.playersMax, R.id.editGameOptions_playerLimit);

        int sl = parseIntOrThrow(scoreLimit, R.id.editGameOptions_scoreLimit);
        checkMaxMin(sl, params.scoreMin, params.scoreMax, R.id.editGameOptions_scoreLimit);

        int bl = 0;
        if (config.blankCardsEnabled()) {
            bl = parseIntOrThrow(blanksLimit, R.id.editGameOptions_blankCards);
            checkMaxMin(bl, params.blankCardsMin, params.blankCardsMax, R.id.editGameOptions_blankCards);
        }

        ArrayList<Integer> cardSetIds = new ArrayList<>();
        for (int i = 0; i < cardSets.getChildCount(); i++) {
            View view = cardSets.getChildAt(i);
            if (view instanceof CheckBox && ((CheckBox) view).isChecked())
                cardSetIds.add(((Deck) view.getTag()).id);
        }

        return new Game.Options(timerMultiplier, vL, pL, sl, bl, cardSetIds, password);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        Window window;
        if (dialog != null && (window = dialog.getWindow()) != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogNewEditGameOptionsBinding.inflate(inflater, container, false);
        options = (Game.Options) requireArguments().getSerializable("options");
        gid = requireArguments().getInt("gid", -1);
        if (gid == -1 || options == null) {
            dismissAllowingStateLoss();
            return null;
        }

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            dismissAllowingStateLoss();
            return null;
        }

        //region General
        CommonUtils.setText(binding.editGameOptionsScoreLimit, String.valueOf(options.scoreLimit));
        CommonUtils.setText(binding.editGameOptionsPlayerLimit, String.valueOf(options.playersLimit));
        CommonUtils.setText(binding.editGameOptionsSpectatorLimit, String.valueOf(options.spectatorsLimit));
        CommonUtils.setText(binding.editGameOptionsPassword, String.valueOf(options.password));

        AutoCompleteTextView timerMultiplierEditText = (AutoCompleteTextView) CommonUtils.getEditText(binding.editGameOptionsTimerMultiplier);
        timerMultiplierEditText.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Game.Options.VALID_TIMER_MULTIPLIERS));
        timerMultiplierEditText.setText(options.timerMultiplier, false);
        timerMultiplierEditText.setValidator(new AutoCompleteTextView.Validator() {
            @Override
            public boolean isValid(CharSequence text) {
                return Game.Options.timerMultiplierIndex(String.valueOf(text)) != -1;
            }

            @Override
            public CharSequence fixText(CharSequence invalidText) {
                return null;
            }
        });

        binding.editGameOptionsBlankCardsContainer.setVisibility(pyx.config().blankCardsEnabled() ? View.VISIBLE : View.GONE);
        CommonUtils.setText(binding.editGameOptionsBlankCards, String.valueOf(options.blanksLimit));
        //endregion

        //region Decks
        binding.editGameOptionsDecksList.removeAllViews();
        for (Deck set : pyx.firstLoad().decks) {
            MaterialCheckBox item = new MaterialCheckBox(getContext());
            item.setTag(set);
            item.setText(set.name);
            item.setChecked(options.cardSets.contains(set.id));
            item.setOnCheckedChangeListener((buttonView, isChecked) -> updateDecksCount());

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = lp.bottomMargin = (int) -TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            binding.editGameOptionsDecksList.addView(item, lp);
        }
        //endregion

        setStatus(Status.GENERAL);
        setLoading(false);

        return binding.getRoot();
    }

    private void updateDecksCount() {
        int count = 0;
        int black = 0;
        int white = 0;
        for (int i = 0; i < binding.editGameOptionsDecksList.getChildCount(); i++) {
            CheckBox view = (CheckBox) binding.editGameOptionsDecksList.getChildAt(i);
            if (view.isChecked()) {
                Deck deck = (Deck) view.getTag();
                count++;
                black += deck.blackCards;
                white += deck.whiteCards;
            }
        }

        binding.editGameOptionsDecksTitle.setText(String.format("%s (%s)", getString(R.string.cardSetsLabel), Utils.buildDeckCountString(count, black, white)));
    }

    private void setLoading(boolean loading) {
        binding.editGameOptionsPrev.setEnabled(!loading);
        binding.editGameOptionsNext.setEnabled(!loading);
        binding.editGameOptionsDecks.setEnabled(!loading);
        binding.editGameOptionsGeneral.setEnabled(!loading);
    }

    @Nullable
    private Game.Options validate() {
        try {
            return validateAndCreate(pyx.config(), pyx.server.params(),
                    CommonUtils.getText(binding.editGameOptionsTimerMultiplier).trim(),
                    CommonUtils.getText(binding.editGameOptionsSpectatorLimit),
                    CommonUtils.getText(binding.editGameOptionsPlayerLimit),
                    CommonUtils.getText(binding.editGameOptionsScoreLimit),
                    CommonUtils.getText(binding.editGameOptionsBlankCards),
                    binding.editGameOptionsDecksList,
                    CommonUtils.getText(binding.editGameOptionsPassword));
        } catch (InvalidFieldException ex) {
            binding.editGameOptionsGeneral.setVisibility(View.VISIBLE);
            binding.editGameOptionsDecks.setVisibility(View.GONE);

            View view = binding.getRoot().findViewById(ex.fieldId);
            if (view instanceof TextInputLayout) {
                if (ex.throwMessage == R.string.outOfRange)
                    ((TextInputLayout) view).setError(getString(R.string.outOfRange, ex.min, ex.max));
                else
                    ((TextInputLayout) view).setError(getString(ex.throwMessage));
            }

            return null;
        }
    }

    private void setStatus(@NotNull Status status) {
        switch (status) {
            case GENERAL:
                binding.editGameOptionsPrev.setText(R.string.cancel);
                binding.editGameOptionsPrev.setOnClickListener(v -> dismissAllowingStateLoss());

                binding.editGameOptionsNext.setText(R.string.next);
                binding.editGameOptionsNext.setOnClickListener(v -> {
                    if (validate() != null) setStatus(Status.DECKS);
                });

                binding.editGameOptionsGeneral.setVisibility(View.VISIBLE);
                binding.editGameOptionsDecks.setVisibility(View.GONE);
                break;
            case DECKS:
                binding.editGameOptionsPrev.setText(R.string.previous);
                binding.editGameOptionsPrev.setOnClickListener(v -> setStatus(Status.GENERAL));

                binding.editGameOptionsNext.setText(R.string.save);
                binding.editGameOptionsNext.setOnClickListener(v -> {
                    binding.editGameOptionsScoreLimit.setErrorEnabled(false);
                    binding.editGameOptionsPlayerLimit.setErrorEnabled(false);
                    binding.editGameOptionsSpectatorLimit.setErrorEnabled(false);
                    binding.editGameOptionsTimerMultiplier.setErrorEnabled(false);
                    binding.editGameOptionsBlankCards.setErrorEnabled(false);
                    binding.editGameOptionsPassword.setErrorEnabled(false);

                    Game.Options newOptions = validate();
                    if (newOptions == null)
                        return;

                    try {
                        setLoading(true);
                        pyx.request(PyxRequests.changeGameOptions(gid, newOptions))
                                .addOnSuccessListener(aVoid -> {
                                    setLoading(false);
                                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.optionsChanged).extra(gid));
                                    dismissAllowingStateLoss();
                                })
                                .addOnFailureListener(ex -> {
                                    setLoading(false);
                                    Log.e(TAG, "Failed changing game options.", ex);
                                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedChangingOptions).extra(gid));
                                });
                    } catch (JSONException ex) {
                        setLoading(false);
                        Log.e(TAG, "Failed crafting options change request.", ex);
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedChangingOptions).extra(gid));
                    }
                });

                binding.editGameOptionsGeneral.setVisibility(View.GONE);
                binding.editGameOptionsDecks.setVisibility(View.VISIBLE);
                updateDecksCount();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private enum Status {
        GENERAL, DECKS
    }

    private static class InvalidFieldException extends Throwable {
        public final int fieldId;
        public final int throwMessage;
        public final int min;
        public final int max;

        InvalidFieldException(@IdRes int fieldId, @StringRes int throwMessage) {
            this.fieldId = fieldId;
            this.throwMessage = throwMessage;
            this.min = -1;
            this.max = -1;
        }

        InvalidFieldException(@IdRes int fieldId, int min, int max) {
            this.fieldId = fieldId;
            this.throwMessage = R.string.outOfRange;
            this.min = min;
            this.max = max;
        }
    }
}
