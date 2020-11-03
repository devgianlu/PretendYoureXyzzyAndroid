package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.databinding.ActivityNewGameBinding;
import com.gianlu.pretendyourexyzzy.game.AnotherGameManager;
import com.gianlu.pretendyourexyzzy.game.GameUi;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

public class GameActivity extends ActivityWithDialog implements AnotherGameManager.Listener, GameUi.Listener {
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

        game = (GamePermalink) getIntent().getSerializableExtra("game");
        if (game == null) {
            finishAfterTransition();
            return;
        }

        ui = new GameUi(this, binding, this);

        try {
            pyx = RegisteredPyx.get();
            manager = new AnotherGameManager(this, game, pyx, ui, this);
        } catch (LevelMismatchException ex) {
            Log.e(TAG, "Failed getting PYX instance.", ex);
            finishAfterTransition();
            return;
        }

        binding.gameActivityClose.setOnClickListener(v -> leaveGame());

        manager.begin();

        AnalyticsApplication.sendAnalytics(Utils.ACTION_JOIN_GAME);

        // TODO: Show loading

        // DialogUtils.showDialog(getActivity(), EditGameOptionsDialog.get(perm.gid, manager.gameOptions()), null);
        // DialogUtils.showDialog(getActivity(), Dialogs.gameOptions(getContext(), options, pyx.firstLoad()));
        // UserInfoDialog.loadAndShow(pyx, activity, player.name);
        // MetricsActivity.startActivity(getContext(), perm);
        // showDialog(GameRoundDialog.get(roundId));
        // showDialog(Dialogs.askDefinitionWord(getContext(), text -> UrbanDictSheet.get().show(getActivity(), text)));
        // CustomDecksSheet.get().show(this, perm.gid);

        /*
         *  if (manager.amHost() && manager.isStatus(Game.Status.LOBBY)) editGameOptions();
         *  else showGameOptions();
         */
    }

    private void leaveGame() {
        pyx.request(PyxRequests.logout())
                .addOnSuccessListener(aVoid -> justLeaveGame())
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed leaving game.", ex);
                    showToast(Toaster.build().message(R.string.failedLeaving).extra(game.gid));
                });
    }

    @Override
    public void onGameLoaded() {
        // TODO: Hide loading
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
    public void onCardSelected(@NonNull BaseCard card) {
        /*
        if (action == GameCardView.Action.SELECT) {
            if (listener != null) listener.onCardSelected(card);
        } else if (action == GameCardView.Action.TOGGLE_STAR) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_STARRED_CARD_ADD);

            BaseCard bc = blackCard();
            if (bc != null && starredCards.putCard((GameCard) bc, group))
                Toaster.with(getContext()).message(R.string.addedCardToStarred).show();
        } else if (action == GameCardView.Action.SELECT_IMG) {
            listener.showDialog(CardImageZoomDialog.get(card));
        }
         */

        if (manager != null) manager.onCardSelected(card);
    }

    @Override
    public void startGame() {
        if (manager != null) manager.startGame();
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
