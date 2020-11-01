package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.databinding.ActivityNewGameBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class GameActivity extends ActivityWithDialog {
    private static final String TAG = GameActivity.class.getSimpleName();
    private int gid;
    private ActivityNewGameBinding binding;
    private RegisteredPyx pyx;

    @NonNull
    private static Intent baseIntent(@NotNull Context context) {
        return new Intent(context, GameActivity.class);
    }

    @NotNull
    public static Intent gameIntent(@NotNull Context context, @NotNull GamePermalink game) {
        Intent intent = baseIntent(context);
        intent.putExtra("gid", game.gid);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNewGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        gid = getIntent().getIntExtra("gid", -1);
        if (gid == -1) {
            finishAfterTransition();
            return;
        }

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Log.e(TAG, "Failed getting PYX instance.", ex);
            finishAfterTransition();
            return;
        }

        binding.gameActivityClose.setOnClickListener(v -> pyx.request(PyxRequests.logout())
                .addOnSuccessListener(aVoid -> finishAfterTransition())
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed leaving game.", ex);
                    showToast(Toaster.build().message(R.string.failedLeaving).extra(gid));
                }));

        // TODO: Game activity
    }

    private void setCounter(int value) {
        binding.gameActivityCounter.setText(String.format(Locale.getDefault(), "%d\u00A0", value));
    }
}
