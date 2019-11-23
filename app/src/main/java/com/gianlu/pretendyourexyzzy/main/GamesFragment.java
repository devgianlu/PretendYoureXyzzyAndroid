package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.GamesList;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.tutorial.Discovery;
import com.gianlu.pretendyourexyzzy.tutorial.GamesTutorial;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

public class GamesFragment extends FragmentWithDialog implements Pyx.OnResult<GamesList>, GamesAdapter.Listener, SearchView.OnCloseListener, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener, Pyx.OnEventListener, TutorialManager.Listener {
    private GamesList lastResult;
    private RecyclerMessageView rmv;
    private OnParticipateGame handler;
    private SearchView searchView;
    private GamesAdapter adapter;
    private GamePermalink launchGame = null;
    private String launchGamePassword = null;
    private boolean launchGameShouldRequest;
    private FloatingActionButton createGame;
    private RegisteredPyx pyx;
    private TutorialManager tutorialManager;

    @NonNull
    public static GamesFragment getInstance() {
        GamesFragment fragment = new GamesFragment();
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);

        if (context instanceof OnParticipateGame)
            handler = (OnParticipateGame) context;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.games_fragment, menu);

        if (getContext() == null) return;
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.gamesFragment_search);
        item.setOnActionExpandListener(this);

        if (searchManager != null && getActivity() != null) {
            searchView = (SearchView) item.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem showLocked = menu.findItem(R.id.gamesFragment_showLocked);
        if (showLocked != null) showLocked.setChecked(!Prefs.getBoolean(PK.FILTER_LOCKED_LOBBIES));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.gamesFragment_showLocked) {
            boolean show = !item.isChecked();
            item.setChecked(show);
            Prefs.putBoolean(PK.FILTER_LOCKED_LOBBIES, !show);
            if (adapter != null) adapter.setFilterOutLockedLobbies(!show);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        onQueryTextSubmit(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (adapter != null) adapter.filterWithQuery(query);
        return true;
    }

    @Override
    public boolean onClose() {
        searchView.setQuery(null, true);
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_games, container, false);
        this.rmv = layout.findViewById(R.id.gamesFragment_recyclerViewLayout);
        this.rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        createGame = layout.findViewById(R.id.gamesFragment_createGame);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            this.rmv.showError(R.string.failedLoading);
            return layout;
        }

        tutorialManager = new TutorialManager(this, Discovery.GAMES);

        this.rmv.enableSwipeRefresh(() -> pyx.request(PyxRequests.getGamesList(), null, GamesFragment.this), R.color.colorAccent);

        createGame.setOnClickListener(v -> {
            DialogUtils.showDialog(getActivity(), DialogUtils.progressDialog(requireContext(), R.string.loading));
            pyx.request(PyxRequests.createGame(), getActivity(), new Pyx.OnResult<GamePermalink>() {
                @Override
                public void onDone(@NonNull GamePermalink result) {
                    DialogUtils.dismissDialog(getActivity());
                    if (handler != null) handler.onParticipatingGame(result);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    DialogUtils.dismissDialog(getActivity());
                    showToast(Toaster.build().message(R.string.failedCreatingGame).ex(ex));
                }
            });
        });

        pyx.request(PyxRequests.getGamesList(), null, this);

        pyx.polling().addListener(this);

        return layout;
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

    public void scrollToTop() {
        if (rmv != null) rmv.list().scrollToPosition(0);
    }

    @Override
    public void onDone(@NonNull GamesList result) {
        if (!isAdded()) return;

        adapter = new GamesAdapter(getContext(), result, pyx, Prefs.getBoolean(PK.FILTER_LOCKED_LOBBIES), this);
        rmv.loadListData(adapter, false);

        lastResult = result;
        updateActivityTitle();
        if (launchGame != null) {
            launchGameInternal(launchGame, launchGamePassword, launchGameShouldRequest);
        } else {
            rmv.list().post(() -> tutorialManager.tryShowingTutorials(getActivity()));
        }
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (lastResult != null && activity != null && isVisible())
            activity.setTitle(getString(R.string.games) + " (" + lastResult.size() + "/" + lastResult.maxGames + ") - " + getString(R.string.app_name));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        if (!PyxException.solveNotRegistered(getContext(), ex))
            rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Override
    public void spectateGame(@NonNull final Game game) {
        if (game.hasPassword(false)) {
            askForPassword(password -> spectateGame(game.gid, password));
        } else {
            spectateGame(game.gid, null);
        }
    }

    @Override
    public void joinGame(@NonNull final Game game) {
        if (game.hasPassword(false)) {
            askForPassword(password -> joinGame(game.gid, password));
        } else {
            joinGame(game.gid, null);
        }
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) rmv.showInfo(R.string.noGames);
        else rmv.showList();
    }

    private void spectateGame(final int gid, @Nullable String password) {
        if (getContext() == null) return;

        DialogUtils.showDialog(getActivity(), DialogUtils.progressDialog(getContext(), R.string.loading));
        pyx.request(PyxRequests.spectateGame(gid, password), getActivity(), new Pyx.OnResult<GamePermalink>() {
            @Override
            public void onDone(@NonNull GamePermalink game) {
                if (handler != null) handler.onParticipatingGame(game);
                DialogUtils.dismissDialog(getActivity());

                AnalyticsApplication.sendAnalytics(Utils.ACTION_SPECTATE_GAME);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.dismissDialog(getActivity());

                if (ex instanceof PyxException) {
                    switch (((PyxException) ex).errorCode) {
                        case "wp":
                            showToast(Toaster.build().message(R.string.wrongPassword).ex(ex).error(false));
                            return;
                        case "gf":
                            showToast(Toaster.build().message(R.string.gameFull).ex(ex).error(false));
                            return;
                    }
                }

                showToast(Toaster.build().message(R.string.failedSpectating).ex(ex));
            }
        });
    }

    private void joinGame(int gid, @Nullable String password) {
        if (getContext() == null) return;

        DialogUtils.showDialog(getActivity(), DialogUtils.progressDialog(getContext(), R.string.loading));
        pyx.request(PyxRequests.joinGame(gid, password), getActivity(), new Pyx.OnResult<GamePermalink>() {
            @Override
            public void onDone(@NonNull GamePermalink game) {
                if (handler != null) handler.onParticipatingGame(game);
                DialogUtils.dismissDialog(getActivity());

                AnalyticsApplication.sendAnalytics(Utils.ACTION_JOIN_GAME);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.dismissDialog(getActivity());

                if (ex instanceof PyxException) {
                    switch (((PyxException) ex).errorCode) {
                        case "wp":
                            showToast(Toaster.build().message(R.string.wrongPassword).ex(ex).error(false));
                            return;
                        case "gf":
                            showToast(Toaster.build().message(R.string.gameFull).ex(ex).error(false));
                            return;
                    }
                }

                showToast(Toaster.build().message(R.string.failedJoining).ex(ex));
            }
        });
    }

    private void askForPassword(@NonNull OnPassword listener) {
        if (getContext() == null) {
            listener.onPassword(null);
            return;
        }

        final EditText password = new EditText(getContext());
        password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.gamePassword)
                .setView(password)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.submit, (dialog, which) -> listener.onPassword(password.getText().toString()));

        DialogUtils.showDialog(getActivity(), builder);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onClose();
        return true;
    }

    public void launchGame(@NonNull GamePermalink perm, @Nullable String password, boolean shouldRequest) {
        if (adapter != null) {
            launchGameInternal(perm, password, shouldRequest);
        } else {
            launchGameShouldRequest = shouldRequest;
            launchGame = perm;
            launchGamePassword = password;
        }
    }

    private void launchGameInternal(GamePermalink perm, @Nullable String password, boolean shouldRequest) {
        Game game = Utils.findGame(adapter.getGames(), perm.gid);
        launchGame = null;
        if (game != null) {
            if (shouldRequest) {
                if (password != null) joinGame(perm.gid, password);
                else joinGame(game);
            } else {
                if (handler != null) handler.onParticipatingGame(perm);
            }
        } else {
            showToast(Toaster.build().message(R.string.failedJoining).ex(new NullPointerException("Couldn't find game for " + perm.gid)));
        }
    }

    public void viewGame(int gid, boolean locked) {
        if (adapter == null) return;

        if (locked && adapter.doesFilterOutLockedLobbies()) {
            Prefs.putBoolean(PK.FILTER_LOCKED_LOBBIES, false);
            adapter.setFilterOutLockedLobbies(false);
        }

        int pos = Utils.indexOf(adapter.getVisibleGames(), gid);
        if (pos != -1) {
            RecyclerView list = rmv.list();
            list.scrollToPosition(pos);
            RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(pos);
            if (holder instanceof GamesAdapter.ViewHolder)
                ((GamesAdapter.ViewHolder) holder).expand.performClick();
        }
    }

    @Override
    public void onPollMessage(@NonNull PollMessage message) {
        if (message.event == PollMessage.Event.GAME_LIST_REFRESH) {
            pyx.request(PyxRequests.getGamesList(), getActivity(), new Pyx.OnResult<GamesList>() {
                @Override
                public void onDone(@NonNull GamesList result) {
                    if (adapter == null) GamesFragment.this.onDone(result);
                    else adapter.itemsChanged(result);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    if (!PyxException.solveNotRegistered(getContext(), ex))
                        showToast(Toaster.build().message(R.string.failedLoading).ex(ex));
                }
            });
        }
    }

    @Override
    public void onStoppedPolling() {
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof GamesTutorial && getActivity() != null && CommonUtils.isVisible(this);
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof GamesTutorial && ((GamesTutorial) tutorial).buildSequence(createGame, rmv.list());
    }

    public interface OnParticipateGame {
        void onParticipatingGame(@NonNull GamePermalink game);
    }

    private interface OnPassword {
        void onPassword(@Nullable String password);
    }
}
