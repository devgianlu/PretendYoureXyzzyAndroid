package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.databinding.DialogNewViewGameOptionsBinding;

import org.jetbrains.annotations.NotNull;

public class NewViewGameOptionsDialog extends DialogFragment {

    @NotNull
    public static NewViewGameOptionsDialog get(@NotNull Game.Options options) {
        NewViewGameOptionsDialog dialog = new NewViewGameOptionsDialog();
        Bundle args = new Bundle();
        args.putSerializable("options", options);
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
        if (options == null) {
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

        return binding.getRoot();
    }
}
