package com.gianlu.pretendyourexyzzy.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.databinding.ActivityNewGameBinding;
import com.gianlu.pretendyourexyzzy.dialogs.GameRoundDialog;
import com.gianlu.pretendyourexyzzy.dialogs.NewChatDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

public class GameActivity extends ActivityWithDialog implements AnotherGameManager.Listener {
    private static final String TAG = GameActivity.class.getSimpleName();
    private GamePermalink game;
    private ActivityNewGameBinding binding;
    private RegisteredPyx pyx;
    private AnotherGameManager manager;
    private GameUi ui;

    @NonNull
    private static Intent baseIntent(@NotNull Context context) {
        return new Intent(context, GameActivity.class);
    }

    @NotNull
    public static Intent gameIntent(@NotNull Context context, @NotNull GamePermalink game) {
        Intent intent = baseIntent(context);
        intent.putExtra("game", game);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNewGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.gameActivityMenu.setOnClickListener(v -> showPopupMenu());

        game = (GamePermalink) getIntent().getSerializableExtra("game");
        if (game == null) {
            finishAfterTransition();
            return;
        }

        try {
            pyx = RegisteredPyx.get();
            ui = new GameUi(this, binding, pyx);
            manager = new AnotherGameManager(this, game, pyx, ui, this);
        } catch (LevelMismatchException ex) {
            Log.e(TAG, "Failed getting PYX instance.", ex);
            finishAfterTransition();
            return;
        }

        if (pyx.config().gameChatEnabled()) {
            binding.gameActivityChat.setVisibility(View.VISIBLE);
            binding.gameActivityChat.setOnClickListener(v -> NewChatDialog.getGame(game.gid).show(getSupportFragmentManager(), null));
        } else {
            binding.gameActivityChat.setVisibility(View.GONE);
        }

        binding.gameActivityClose.setOnClickListener(v -> leaveGame());
        binding.gameActivityLoading.setVisibility(View.VISIBLE);

        manager.begin();

        setKeepScreenOn(Prefs.getBoolean(PK.KEEP_SCREEN_ON));
    }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, binding.gameActivityMenu);
        popup.inflate(R.menu.game);

        Menu menu = popup.getMenu();
        menu.findItem(R.id.game_keepScreenOn).setChecked(Prefs.getBoolean(PK.KEEP_SCREEN_ON));

        if (manager == null || manager.getLastRoundMetricsId() == null)
            menu.removeItem(R.id.game_lastRound);

        if (pyx == null || !pyx.hasMetrics())
            menu.removeItem(R.id.game_gameMetrics);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.game_keepScreenOn) {
                boolean on = !item.isChecked();
                item.setChecked(on);
                setKeepScreenOn(on);
                return true;
            } else if (item.getItemId() == R.id.game_gameMetrics) {
                // TODO: Show game metrics
                return true;
            } else if (item.getItemId() == R.id.game_lastRound) {
                if (manager == null)
                    return false;

                String roundId = manager.getLastRoundMetricsId();
                if (roundId != null) {
                    showDialog(GameRoundDialog.get(roundId));
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        });
        popup.show();
    }

    private void setKeepScreenOn(boolean on) {
        Prefs.putBoolean(PK.KEEP_SCREEN_ON, on);

        Window window = getWindow();
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void leaveGame() {
        pyx.request(PyxRequests.leaveGame(game.gid))
                .addOnSuccessListener(aVoid -> justLeaveGame())
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed leaving game.", ex);
                    showToast(Toaster.build().message(R.string.failedLeaving).extra(game.gid));
                });
    }

    @Override
    public void onGameLoaded() {
        binding.gameActivityLoading.setVisibility(View.GONE);
    }

    @Override
    public void onFailedLoadingGame(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading game.", ex);
        Toaster.with(this).message(R.string.failedLoadingGame).extra(game.gid).show();
        finishAfterTransition();
    }

    @Override
    public void justLeaveGame() {
        AnalyticsApplication.sendAnalytics(Utils.ACTION_LEFT_GAME);
        finishAfterTransition();
    }

    @Override
    public void onBackPressed() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.leaveGame)
                .setMessage(R.string.leaveGame_confirm)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> leaveGame())
                .setNegativeButton(android.R.string.no, null);

        showDialog(builder);
    }
}
