package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.NameValuePair;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.dialogs.Dialogs;
import com.gianlu.pretendyourexyzzy.dialogs.EditGameOptionsDialog;
import com.gianlu.pretendyourexyzzy.dialogs.GameRoundDialog;
import com.gianlu.pretendyourexyzzy.dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.AnotherGameManager;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.CustomDecksSheet;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.GameLayout;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.UrbanDictSheet;
import com.gianlu.pretendyourexyzzy.metrics.MetricsActivity;
import com.gianlu.pretendyourexyzzy.tutorial.CreateGameTutorial;
import com.gianlu.pretendyourexyzzy.tutorial.Discovery;
import com.gianlu.pretendyourexyzzy.tutorial.HowToPlayTutorial;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import okhttp3.HttpUrl;

public class OngoingGameFragment extends FragmentWithDialog implements PlayersAdapter.Listener, TutorialManager.Listener, AnotherGameManager.Listener, AnotherGameManager.OnPlayerStateChanged {
    private static final String TAG = OngoingGameFragment.class.getSimpleName();
    private OnLeftGame onLeftGame;
    private ProgressBar loading;
    private GameLayout gameLayout;
    private GamePermalink perm;
    private RegisteredPyx pyx;
    private MessageView message;
    private TutorialManager tutorialManager;
    private AnotherGameManager manager;
    private CustomDecksSheet customDecksSheet;

    @NonNull
    public static OngoingGameFragment getInstance(@NonNull GamePermalink game) {
        OngoingGameFragment fragment = new OngoingGameFragment();
        fragment.setHasOptionsMenu(true);
        Bundle args = new Bundle();
        args.putSerializable("game", game);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnLeftGame)
            onLeftGame = (OnLeftGame) context;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) updateActivityTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityTitle();
    }

    @Override
    public void updateActivityTitle() {
        Activity activity = getActivity();
        if (manager != null && activity != null && isVisible())
            activity.setTitle(manager.host() + " - " + getString(R.string.app_name));
    }

    @Override
    public void justLeaveGame() {
        if (onLeftGame != null) onLeftGame.onLeftGame();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem lastRound = menu.findItem(R.id.ongoingGame_lastRound);
        if (lastRound != null)
            lastRound.setVisible(manager != null && manager.getLastRoundMetricsId() != null);

        MenuItem gameMetrics = menu.findItem(R.id.ongoingGame_gameMetrics);
        if (gameMetrics != null)
            gameMetrics.setVisible(perm.gamePermalink != null);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ongoing_game, menu);
    }

    private void leaveGame() {
        if (pyx == null)
            return;

        pyx.request(PyxRequests.leaveGame(perm.gid), getActivity(), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                if (onLeftGame != null) onLeftGame.onLeftGame();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (ex instanceof PyxException && (((PyxException) ex).errorCode.equals("nitg") || ((PyxException) ex).errorCode.equals("ig"))) {
                    onDone();
                } else {
                    Log.e(TAG, "Failed leaving game.", ex);
                    showToast(Toaster.build().message(R.string.failedLeaving));
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (manager != null) manager.destroy();
        super.onDestroy();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.fragment_ongoing_game, parent, false);
        loading = layout.findViewById(R.id.ongoingGame_loading);
        gameLayout = layout.findViewById(R.id.ongoingGame_gameLayout);
        gameLayout.attach(this);
        message = layout.findViewById(R.id.ongoingGame_message);

        Bundle args = getArguments();
        if (args == null || (perm = (GamePermalink) args.getSerializable("game")) == null) {
            loading.setVisibility(View.GONE);
            gameLayout.setVisibility(View.GONE);
            message.error(R.string.failedLoading);
            return layout;
        }

        tutorialManager = new TutorialManager(this, Discovery.CREATE_GAME, Discovery.HOW_TO_PLAY);

        try {
            pyx = RegisteredPyx.get();
            manager = new AnotherGameManager(perm, pyx, gameLayout, this);
        } catch (LevelMismatchException ex) {
            loading.setVisibility(View.GONE);
            gameLayout.setVisibility(View.GONE);
            message.error(R.string.failedLoading);
            return layout;
        }

        manager.begin();
        manager.setPlayerStateChangedListener(this);

        return layout;
    }

    private void refresh() {
        if (manager != null) {
            manager.reset();
            manager = null;
        }

        manager = new AnotherGameManager(perm, pyx, gameLayout, this);
        manager.begin();
    }

    public boolean canModifyCustomDecks() {
        return manager != null && manager.amHost() && manager.isStatus(Game.Status.LOBBY);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (getContext() == null) return false;

        switch (item.getItemId()) {
            case R.id.ongoingGame_refresh:
                refresh();
                return true;
            case R.id.ongoingGame_leave:
                leaveGame();
                return true;
            case R.id.ongoingGame_options:
                if (manager.amHost() && manager.isStatus(Game.Status.LOBBY)) editGameOptions();
                else showGameOptions();
                return true;
            case R.id.ongoingGame_customDecks:
                if (customDecksSheet != null) customDecksSheet.dismissAllowingStateLoss();
                customDecksSheet = CustomDecksSheet.get();
                customDecksSheet.show(this, perm.gid);
                return true;
            case R.id.ongoingGame_spectators:
                showSpectators();
                return true;
            case R.id.ongoingGame_players:
                showPlayers();
                return true;
            case R.id.ongoingGame_share:
                shareGame();
                return true;
            case R.id.ongoingGame_gameMetrics:
                MetricsActivity.startActivity(getContext(), perm);
                return true;
            case R.id.ongoingGame_lastRound:
                String roundId = manager == null ? null : manager.getLastRoundMetricsId();
                if (roundId != null) showDialog(GameRoundDialog.get(roundId));
                return true;
            case R.id.ongoingGame_definition:
                showDialog(Dialogs.askDefinitionWord(getContext(), text -> UrbanDictSheet.get().show(getActivity(), text)));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPlayers() {
        if (manager == null || getContext() == null) return;

        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new PlayersAdapter(getContext(), manager.players(), this));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.playersLabel)
                .setView(recyclerView)
                .setPositiveButton(android.R.string.ok, null);

        DialogUtils.showDialog(getActivity(), builder);
    }

    private void shareGame() {
        if (manager == null) return;
        Game.Options options = manager.gameOptions();
        if (options == null) return;

        HttpUrl.Builder builder = pyx.server.url.newBuilder();
        builder.addPathSegment("game.jsp");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("game", String.valueOf(perm.gid)));
        if (manager.hasPassword(true))
            params.add(new NameValuePair("password", options.password));

        builder.fragment(Utils.formQuery(params));

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, builder.toString());
        startActivity(Intent.createChooser(i, "Share game..."));
    }

    private void showSpectators() {
        if (manager == null || getContext() == null) return;

        Collection<String> spectators = manager.spectators();
        SuperTextView text = SuperTextView.html(getContext(), spectators.isEmpty() ? "<i>none</i>" : CommonUtils.join(spectators, ", "));
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        text.setPadding(padding, padding, padding, padding);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.spectatorsLabel)
                .setView(text)
                .setPositiveButton(android.R.string.ok, null);

        DialogUtils.showDialog(getActivity(), builder);
    }

    private void editGameOptions() {
        if (manager == null || getContext() == null) return;

        DialogUtils.showDialog(getActivity(), EditGameOptionsDialog.get(perm.gid, manager.gameOptions()), null);
    }

    private void showGameOptions() {
        if (manager == null || getContext() == null) return;
        Game.Options options = manager.gameOptions();
        if (options == null) return;

        DialogUtils.showDialog(getActivity(), Dialogs.gameOptions(getContext(), options, pyx.firstLoad()));
    }

    public void goBack() {
        if (customDecksSheet != null && customDecksSheet.isAdded() && customDecksSheet.isVisible()) {
            customDecksSheet.dismissAllowingStateLoss();
            customDecksSheet = null;
            return;
        }

        if (isVisible() && DialogUtils.hasVisibleDialog(getActivity())) {
            DialogUtils.dismissDialog(getActivity());
            return;
        }

        if (getContext() == null) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.leaveGame)
                .setMessage(R.string.leaveGame_confirm)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> leaveGame())
                .setNegativeButton(android.R.string.no, null);

        DialogUtils.showDialog(getActivity(), builder);
    }

    @Override
    public void onPlayerSelected(@NonNull GameInfo.Player player) {
        final FragmentActivity activity = getActivity();
        if (activity != null) UserInfoDialog.loadAndShow(pyx, activity, player.name);
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        if (getActivity() == null || !CommonUtils.isVisible(this)) return false;

        if (tutorial instanceof CreateGameTutorial) {
            return manager != null && manager.amHost();
        } else if (tutorial instanceof HowToPlayTutorial) {
            return manager != null && manager.isPlayerStatus(GameInfo.PlayerStatus.PLAYING);
        } else {
            return false;
        }
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        if (tutorial instanceof CreateGameTutorial) {
            return ((CreateGameTutorial) tutorial).buildSequence(requireActivity(), gameLayout);
        } else if (tutorial instanceof HowToPlayTutorial) {
            return ((HowToPlayTutorial) tutorial).buildSequence(gameLayout.getBlackCardView(),
                    gameLayout.getCardsRecyclerView(), gameLayout.getPlayersRecyclerView());
        } else {
            return false;
        }
    }

    @Override
    public void onPlayerStateChanged(@NonNull GameInfo.PlayerStatus status) {
        gameLayout.getCardsRecyclerView().post(() -> tutorialManager.tryShowingTutorials(getActivity()));
    }

    @Override
    public void onGameLoaded() {
        updateActivityTitle();

        loading.setVisibility(View.GONE);
        gameLayout.setVisibility(View.VISIBLE);
        message.hide();

        tutorialManager.tryShowingTutorials(getActivity());
    }

    @Override
    public void onFailedLoadingGame(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading game.", ex);
        loading.setVisibility(View.GONE);
        gameLayout.setVisibility(View.GONE);
        message.error(R.string.failedLoading_reason, ex.getMessage());
    }
}
