package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.NewEditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.customdecks.NewViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.databinding.DialogNewViewGameOptionsBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class NewViewGameOptionsDialog extends DialogFragment {

    @NotNull
    public static NewViewGameOptionsDialog get(@NotNull Game.Options options, @NotNull ArrayList<Deck> customDecks) {
        NewViewGameOptionsDialog dialog = new NewViewGameOptionsDialog();
        Bundle args = new Bundle();
        args.putSerializable("options", options);
        args.putSerializable("customDecks", customDecks);
        dialog.setArguments(args);
        return dialog;
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
        DialogNewViewGameOptionsBinding binding = DialogNewViewGameOptionsBinding.inflate(inflater, container, false);
        binding.viewGameOptionsOk.setOnClickListener(v -> dismissAllowingStateLoss());

        Game.Options options = (Game.Options) requireArguments().getSerializable("options");
        ArrayList<Deck> customDecks = (ArrayList<Deck>) requireArguments().getSerializable("customDecks");
        if (options == null || customDecks == null) {
            dismissAllowingStateLoss();
            return null;
        }

        RegisteredPyx pyx;
        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            dismissAllowingStateLoss();
            return null;
        }

        binding.viewGameOptionsScoreLimit.setText(String.valueOf(options.scoreLimit));
        binding.viewGameOptionsPlayerLimit.setText(String.valueOf(options.playersLimit));
        binding.viewGameOptionsSpectatorLimit.setText(String.valueOf(options.spectatorsLimit));
        binding.viewGameOptionsTimerMultiplier.setText(String.valueOf(options.timerMultiplier));
        binding.viewGameOptionsBlankCards.setText(String.valueOf(options.blanksLimit));

        if (options.password == null || options.password.isEmpty()) {
            ((View) binding.viewGameOptionsPassword.getParent()).setVisibility(View.GONE);
        } else {
            ((View) binding.viewGameOptionsPassword.getParent()).setVisibility(View.VISIBLE);
            binding.viewGameOptionsPassword.setText(options.password);
        }

        binding.viewGameOptionsDecks.removeAllViews();
        binding.viewGameOptionsCardSetsCount.setText(String.valueOf(options.cardSets.size()));
        for (Integer deckId : options.cardSets) {
            TextView view = new TextView(requireContext());
            view.setTextAppearance(requireContext(), R.style.TextAppearance_Regular);
            view.setTextColor(Color.rgb(134, 134, 134));
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            view.setText(String.format("• %s", pyx.firstLoad().cardSetName(deckId)));
            binding.viewGameOptionsDecks.addView(view);
        }

        binding.viewGameOptionsCustomDecks.removeAllViews();
        if (!pyx.config().customDecksEnabled() && !pyx.config().crCastEnabled()) {
            ((View) binding.viewGameOptionsCustomDecksCount.getParent()).setVisibility(View.GONE);
        } else {
            ((View) binding.viewGameOptionsCustomDecksCount.getParent()).setVisibility(View.VISIBLE);
            binding.viewGameOptionsCustomDecksCount.setText(String.valueOf(customDecks.size()));

            for (Deck deck : customDecks) {
                TextView view = new TextView(requireContext());
                view.setTextAppearance(requireContext(), R.style.TextAppearance_Regular);
                view.setTextColor(Color.rgb(134, 134, 134));
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                view.setText(Html.fromHtml(String.format("• %s (<i>%s</i>)", deck.name, deck.watermark)));
                binding.viewGameOptionsCustomDecks.addView(view);

                // TODO: View deck?
            }
        }

        return binding.getRoot();
    }

    public void onDeckSelected(@NonNull Deck deck) {
        CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());

        Intent intent = null;
        List<CustomDecksDatabase.CustomDeck> decks = db.getDecks();
        for (CustomDecksDatabase.CustomDeck customDeck : decks) {
            if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark)) {
                intent = NewEditCustomDeckActivity.activityEditIntent(requireContext(), customDeck);
                break;
            }
        }

        if (intent == null) {
            List<CrCastDeck> crCastDecks = db.getCachedCrCastDecks();
            for (CrCastDeck customDeck : crCastDecks) {
                if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark)) {
                    intent = NewViewCustomDeckActivity.activityCrCastIntent(requireContext(), customDeck);
                    break;
                }
            }
        }

        if (intent == null) {
            List<CustomDecksDatabase.StarredDeck> starredDecks = db.getStarredDecks(false);
            for (CustomDecksDatabase.StarredDeck customDeck : starredDecks) {
                if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark) && customDeck.owner != null) {
                    intent = NewViewCustomDeckActivity.activityPublicIntent(requireContext(), customDeck);
                    break;
                }
            }
        }

        if (intent == null) {
            if (OverloadedUtils.isSignedIn())
                intent = NewViewCustomDeckActivity.activitySearchIntent(requireContext(), deck);
            else
                DialogUtils.showToast(requireContext(), Toaster.build().message(R.string.featureOverloadedOnly));
        }

        if (intent != null) startActivity(intent);
    }
}
