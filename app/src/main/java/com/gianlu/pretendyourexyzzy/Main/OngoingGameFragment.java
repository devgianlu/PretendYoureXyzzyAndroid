package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CasualViews.MessageView;
import com.gianlu.commonutils.CasualViews.SuperTextView;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NameValuePair;
import com.gianlu.commonutils.Toaster;
import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.commonutils.Tutorial.TutorialManager;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.Dialogs.Dialogs;
import com.gianlu.pretendyourexyzzy.Dialogs.EditGameOptionsDialog;
import com.gianlu.pretendyourexyzzy.Dialogs.GameRoundDialog;
import com.gianlu.pretendyourexyzzy.Dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.Main.OngoingGame.AnotherGameManager;
import com.gianlu.pretendyourexyzzy.Main.OngoingGame.CardcastSheet;
import com.gianlu.pretendyourexyzzy.Main.OngoingGame.GameLayout;
import com.gianlu.pretendyourexyzzy.Main.OngoingGame.UrbanDictSheet;
import com.gianlu.pretendyourexyzzy.Metrics.MetricsActivity;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Deck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamePermalink;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Starred.StarredDecksManager;
import com.gianlu.pretendyourexyzzy.Tutorial.CreateGameTutorial;
import com.gianlu.pretendyourexyzzy.Tutorial.Discovery;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import okhttp3.HttpUrl;

public class OngoingGameFragment extends FragmentWithDialog implements OngoingGameHelper.Listener, PlayersAdapter.Listener, TutorialManager.Listener, AnotherGameManager.Listener {
    private OnLeftGame onLeftGame;
    private ProgressBar loading;
    private GameLayout gameLayout;
    private GamePermalink perm;
    private RegisteredPyx pyx;
    private Cardcast cardcast;
    private CardcastSheet cardcastSheet;
    private MessageView message;
    private TutorialManager tutorialManager;
    private AnotherGameManager manager;

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
    public void onAttach(Context context) {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ongoing_game, menu);
    }

    private void leaveGame() {
        if (pyx != null)
            pyx.request(PyxRequests.leaveGame(perm.gid), getActivity(), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                if (onLeftGame != null) onLeftGame.onLeftGame();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (ex instanceof PyxException && ((PyxException) ex).errorCode.equals("nitg"))
                    onDone();
                else
                    showToast(Toaster.build().message(R.string.failedLeaving).ex(ex));
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

        tutorialManager = new TutorialManager(this, Discovery.CREATE_GAME);

        try {
            pyx = RegisteredPyx.get();
            cardcast = Cardcast.get();
            manager = new AnotherGameManager(perm, pyx, gameLayout, this);
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            loading.setVisibility(View.GONE);
            gameLayout.setVisibility(View.GONE);
            message.error(R.string.failedLoading);
            return layout;
        }

        manager.begin();

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                if (roundId != null && getActivity() != null)
                    GameRoundDialog.get(roundId).show(getActivity().getSupportFragmentManager(), null);
                return true;
            case R.id.ongoingGame_cardcast:
                cardcastSheet = CardcastSheet.get();
                cardcastSheet.show(getActivity(), perm.gid);
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

        builder.fragment(CommonUtils.formQuery(params));

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, builder.toString());
        startActivity(Intent.createChooser(i, "Share game..."));
    }

    private void showSpectators() {
        if (manager == null || getContext() == null) return;

        Collection<String> spectators = manager.spectators();
        SuperTextView text = new SuperTextView(getContext(), R.string.spectatorsList, spectators.isEmpty() ? "none" : CommonUtils.join(spectators, ", "));
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

        DialogUtils.showDialog(getActivity(), EditGameOptionsDialog.get(perm.gid, manager.gameOptions()));
    }

    private void showGameOptions() {
        if (manager == null || getContext() == null) return;
        Game.Options options = manager.gameOptions();
        if (options == null) return;

        DialogUtils.showDialog(getActivity(), Dialogs.gameOptions(getContext(), options, pyx.firstLoad()));
    }

    @Override
    public void addCardcastDeck(@NonNull String code) {
        if (code.length() != 5) {
            showToast(Toaster.build().message(R.string.invalidCardcastCode).extra(code));
            return;
        }

        pyx.addCardcastDeckAndList(perm.gid, code, cardcast, getActivity(), new Pyx.OnResult<List<Deck>>() {
            @Override
            public void onDone(@NonNull List<Deck> result) {
                showToast(Toaster.build().message(R.string.cardcastAdded));
                AnalyticsApplication.sendAnalytics(Utils.ACTION_ADDED_CARDCAST);

                if (cardcastSheet != null) cardcastSheet.update(result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                showToast(Toaster.build().message(R.string.failedAddingCardcast).ex(ex));
            }
        });
    }

    public void goBack() {
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
    public boolean canModifyCardcastDecks() {
        return manager != null && manager.amHost() && manager.isStatus(Game.Status.LOBBY);
    }

    @Override
    public void addCardcastStarredDecks() {
        if (getContext() == null) return;
        List<StarredDecksManager.StarredDeck> starredDecks = StarredDecksManager.get().getDecks();
        if (starredDecks.isEmpty()) {
            showToast(Toaster.build().message(R.string.noStarredDecks));
            return;
        }

        List<String> codes = new ArrayList<>();
        for (StarredDecksManager.StarredDeck deck : starredDecks)
            codes.add(deck.code);

        pyx.addCardcastDecksAndList(perm.gid, codes, cardcast, getActivity(), new Pyx.OnResult<List<Deck>>() {
            @Override
            public void onDone(@NonNull List<Deck> result) {
                showToast(Toaster.build().message(R.string.starredDecksAdded));
                AnalyticsApplication.sendAnalytics(Utils.ACTION_ADDED_CARDCAST);

                if (cardcastSheet != null) cardcastSheet.update(result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (ex instanceof RegisteredPyx.PartialCardcastAddFail) {
                    showToast(Toaster.build().message(R.string.addStarredDecksFailedPartial).ex(ex).extra(((RegisteredPyx.PartialCardcastAddFail) ex).getCodes()));
                    return;
                }

                showToast(Toaster.build().message(R.string.failedAddingCardcast).ex(ex));
            }
        });
    }

    @Override
    public void onPlayerSelected(@NonNull GameInfo.Player player) {
        final FragmentActivity activity = getActivity();
        if (activity != null) UserInfoDialog.loadAndShow(pyx, activity, player.name);
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof CreateGameTutorial && getActivity() != null && manager != null && CommonUtils.isVisible(this) && manager.amHost();
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof CreateGameTutorial && ((CreateGameTutorial) tutorial).buildSequence(requireActivity(), gameLayout);
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
        Logging.log(ex);
        loading.setVisibility(View.GONE);
        gameLayout.setVisibility(View.GONE);
        message.error(R.string.failedLoading_reason, ex.getMessage());
    }
}
