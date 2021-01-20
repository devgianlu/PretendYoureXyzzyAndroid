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
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequest;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.api.models.CahConfig;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.databinding.DialogNewEditGameOptionsBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class NewEditGameOptionsDialog extends DialogFragment {
    private static final String TAG = NewEditGameOptionsDialog.class.getSimpleName();
    private DialogNewEditGameOptionsBinding binding;
    private RegisteredPyx pyx;
    private int gid;
    private ArrayList<Deck> inUseCustomDecks;

    @NotNull
    public static NewEditGameOptionsDialog get(int gid, @NotNull Game.Options options, @NotNull ArrayList<Deck> customDecks, boolean goToCustomDecks) {
        NewEditGameOptionsDialog dialog = new NewEditGameOptionsDialog();
        Bundle args = new Bundle();
        args.putInt("gid", gid);
        args.putBoolean("goToCustomDecks", goToCustomDecks);
        args.putSerializable("options", options);
        args.putSerializable("customDecks", customDecks);
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
    @SuppressWarnings("unchecked")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogNewEditGameOptionsBinding.inflate(inflater, container, false);
        Game.Options options = (Game.Options) requireArguments().getSerializable("options");
        gid = requireArguments().getInt("gid", -1);
        inUseCustomDecks = (ArrayList<Deck>) requireArguments().getSerializable("customDecks");
        if (gid == -1 || options == null || inUseCustomDecks == null) {
            dismissAllowingStateLoss();
            return null;
        }

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            dismissAllowingStateLoss();
            return null;
        }

        boolean goToCustomDecks = requireArguments().getBoolean("goToCustomDecks", false)
                && (pyx.config().customDecksEnabled() || pyx.config().crCastEnabled());

        //region General
        CommonUtils.setText(binding.editGameOptionsScoreLimit, String.valueOf(options.scoreLimit));
        CommonUtils.setText(binding.editGameOptionsPlayerLimit, String.valueOf(options.playersLimit));
        CommonUtils.setText(binding.editGameOptionsSpectatorLimit, String.valueOf(options.spectatorsLimit));
        CommonUtils.setText(binding.editGameOptionsPassword, String.valueOf(options.password));

        AutoCompleteTextView timerMultiplierEditText = (AutoCompleteTextView) CommonUtils.getEditText(binding.editGameOptionsTimerMultiplier);
        timerMultiplierEditText.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Game.Options.VALID_TIMER_MULTIPLIERS));
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

        //region Custom decks
        binding.editGameOptionsCustomDecksList.removeAllViews();
        List<BasicCustomDeck> customDecks = getAddableCustomDecks(CustomDecksDatabase.get(requireContext()), pyx.config().crCastEnabled());
        if (customDecks.isEmpty()) {
            binding.editGameOptionsCustomDecksEmpty.setVisibility(View.VISIBLE);
            binding.editGameOptionsCustomDecksList.setVisibility(View.GONE);
        } else {
            binding.editGameOptionsCustomDecksEmpty.setVisibility(View.GONE);
            binding.editGameOptionsCustomDecksList.setVisibility(View.VISIBLE);

            for (BasicCustomDeck deck : customDecks) {
                MaterialCheckBox item = new MaterialCheckBox(getContext());
                item.setTag(deck);
                item.setText(HtmlCompat.fromHtml(String.format("%s (<i>%s</i>)", deck.name, deck.watermark), HtmlCompat.FROM_HTML_MODE_LEGACY));
                item.setChecked(Deck.contains(inUseCustomDecks, deck));
                item.setOnCheckedChangeListener((buttonView, isChecked) -> updateCustomDecksCount());

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = lp.bottomMargin = (int) -TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
                binding.editGameOptionsCustomDecksList.addView(item, lp);
            }
        }
        //endregion

        setStatus(goToCustomDecks ? Status.CUSTOM_DECKS : Status.GENERAL);
        setLoading(false);

        return binding.getRoot();
    }

    @NonNull
    private List<BasicCustomDeck> getAddableCustomDecks(@NonNull CustomDecksDatabase db, boolean needsUrl) {
        List<Class<? extends BasicCustomDeck>> supported = new ArrayList<>(4);
        if (pyx.config().customDecksEnabled())
            supported.addAll(Arrays.asList(CustomDecksDatabase.CustomDeck.class, CustomDecksDatabase.StarredDeck.class, CrCastDeck.class));
        else if (pyx.config().crCastEnabled())
            supported.add(CrCastDeck.class);

        if (supported.isEmpty())
            return Collections.emptyList();

        List<BasicCustomDeck> customDecks = db.getAllDecks();
        Iterator<BasicCustomDeck> iter = customDecks.iterator();
        while (iter.hasNext()) {
            BasicCustomDeck deck = iter.next();
            if (!supported.contains(deck.getClass())) {
                iter.remove();
            } else {
                if (deck instanceof CrCastDeck && !((CrCastDeck) deck).isAccepted() && needsUrl)
                    iter.remove();
            }
        }

        Collections.sort(customDecks, (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
        return customDecks;
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

    private void updateCustomDecksCount() {
        int count = 0;
        int black = 0;
        int white = 0;
        for (int i = 0; i < binding.editGameOptionsCustomDecksList.getChildCount(); i++) {
            CheckBox view = (CheckBox) binding.editGameOptionsCustomDecksList.getChildAt(i);
            if (view.isChecked()) {
                BasicCustomDeck deck = (BasicCustomDeck) view.getTag();
                count++;
                black += deck.blackCardsCount();
                white += deck.whiteCardsCount();
            }
        }

        binding.editGameOptionsCustomDecksTitle.setText(String.format("%s (%s)", getString(R.string.customDecks), Utils.buildDeckCountString(count, black, white)));
    }

    private void setLoading(boolean loading) {
        binding.editGameOptionsPrev.setEnabled(!loading);
        binding.editGameOptionsNext.setEnabled(!loading);
        binding.editGameOptionsDecks.setEnabled(!loading);
        binding.editGameOptionsGeneral.setEnabled(!loading);
        binding.editGameOptionsCustomDecks.setEnabled(!loading);
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
                binding.editGameOptionsCustomDecks.setVisibility(View.GONE);
                binding.editGameOptionsDecks.setVisibility(View.GONE);
                break;
            case DECKS:
                binding.editGameOptionsPrev.setText(R.string.previous);
                binding.editGameOptionsPrev.setOnClickListener(v -> setStatus(Status.GENERAL));

                if (pyx.config().customDecksEnabled() || pyx.config().crCastEnabled()) {
                    binding.editGameOptionsNext.setText(R.string.next);
                    binding.editGameOptionsNext.setOnClickListener(v -> setStatus(Status.CUSTOM_DECKS));
                } else {
                    binding.editGameOptionsNext.setText(R.string.save);
                    binding.editGameOptionsNext.setOnClickListener(v -> save());
                }

                binding.editGameOptionsGeneral.setVisibility(View.GONE);
                binding.editGameOptionsCustomDecks.setVisibility(View.GONE);
                binding.editGameOptionsDecks.setVisibility(View.VISIBLE);
                updateDecksCount();
                break;
            case CUSTOM_DECKS:
                binding.editGameOptionsPrev.setText(R.string.previous);
                binding.editGameOptionsPrev.setOnClickListener(v -> setStatus(Status.DECKS));

                binding.editGameOptionsNext.setText(R.string.save);
                binding.editGameOptionsNext.setOnClickListener(v -> save());

                binding.editGameOptionsGeneral.setVisibility(View.GONE);
                binding.editGameOptionsDecks.setVisibility(View.GONE);
                binding.editGameOptionsCustomDecks.setVisibility(View.VISIBLE);
                updateCustomDecksCount();
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @NotNull
    private List<Task<Void>> customDecksDiffTasks() {
        List<BasicCustomDeck> selected = new ArrayList<>(10);
        for (int i = 0; i < binding.editGameOptionsCustomDecksList.getChildCount(); i++) {
            View view = binding.editGameOptionsCustomDecksList.getChildAt(i);
            if (view instanceof CheckBox && ((CheckBox) view).isChecked())
                selected.add((BasicCustomDeck) view.getTag());
        }

        List<Deck> toRemove = new ArrayList<>(10);
        for (Deck inUse : inUseCustomDecks) {
            if (!BasicCustomDeck.contains(selected, inUse))
                toRemove.add(inUse);
        }

        List<BasicCustomDeck> toAdd = new ArrayList<>(10);
        for (BasicCustomDeck deck : selected) {
            if (!Deck.contains(inUseCustomDecks, deck))
                toAdd.add(deck);
        }

        if (toRemove.isEmpty() && toAdd.isEmpty())
            return new ArrayList<>(1);

        List<Task<Void>> tasks = new ArrayList<>(toAdd.size() + toRemove.size() + 1);

        //region To remove
        for (Deck deck : toRemove) {
            PyxRequest req = null;
            if (pyx.config().customDecksEnabled())
                req = PyxRequests.removeCustomDeck(gid, deck.id);
            else if (pyx.config().crCastEnabled())
                req = PyxRequests.removeCrCastDeck(gid, Integer.toString(-deck.id, 36));

            if (req == null)
                continue;

            tasks.add(pyx.request(req));
        }
        //endregion

        //region To add
        CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());
        for (BasicCustomDeck deck : toAdd) {
            Task<Void> task;
            if (pyx.config().customDecksEnabled()) {
                if (deck instanceof CustomDecksDatabase.CustomDeck) {
                    db.updateDeckLastUsed(((CustomDecksDatabase.CustomDeck) deck).id);
                    try {
                        String json = ((CustomDecksDatabase.CustomDeck) deck).craftPyxJson(db).toString();
                        task = pyx.request(PyxRequests.addCustomDeckJson(gid, json));
                    } catch (JSONException ex) {
                        Log.e(TAG, "Failed crating JSON for deck: " + deck.name, ex);
                        task = Tasks.forException(ex);
                    }
                } else if (deck instanceof CustomDecksDatabase.StarredDeck) {
                    db.updateStarredDeckLastUsed(((CustomDecksDatabase.StarredDeck) deck).id);
                    String url = OverloadedUtils.getServeCustomDeckUrl(((CustomDecksDatabase.StarredDeck) deck).shareCode);
                    task = pyx.request(PyxRequests.addCustomDeckUrl(gid, url));
                } else if (deck instanceof CrCastDeck) {
                    db.updateCrCastDeckLastUsed(deck.watermark, System.currentTimeMillis());
                    if (((CrCastDeck) deck).isAccepted()) {
                        String url = CrCastApi.getDeckUrl((CrCastDeck) deck);
                        task = pyx.request(PyxRequests.addCustomDeckUrl(gid, url));
                    } else {
                        task = ((CrCastDeck) deck).getCards(db)
                                .continueWithTask(task1 -> {
                                    String json;
                                    try {
                                        json = task1.getResult().craftPyxJson().toString();
                                    } catch (JSONException ex) {
                                        Log.e(TAG, "Failed crating JSON for CrCast deck: " + deck.watermark, ex);
                                        return Tasks.forException(ex);
                                    }

                                    return pyx.request(PyxRequests.addCustomDeckJson(gid, json));
                                });
                    }
                } else {
                    throw new IllegalStateException(deck.toString());
                }
            } else if (pyx.config().crCastEnabled()) {
                if (deck instanceof CrCastDeck) {
                    db.updateCrCastDeckLastUsed(deck.watermark, System.currentTimeMillis());
                    task = pyx.request(PyxRequests.addCrCastDeck(gid, deck.watermark));
                } else {
                    throw new IllegalStateException(deck.toString());
                }
            } else {
                task = null;
            }

            if (task != null) tasks.add(task);
        }
        //endregion

        return tasks;
    }

    private void save() {
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
            List<Task<Void>> tasks = customDecksDiffTasks();
            tasks.add(pyx.request(PyxRequests.changeGameOptions(gid, newOptions)));
            Tasks.whenAll(tasks)
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
    }

    private enum Status {
        GENERAL, DECKS, CUSTOM_DECKS
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
