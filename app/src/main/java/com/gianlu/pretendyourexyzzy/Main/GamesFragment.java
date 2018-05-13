package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SearchView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.PKeys;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.TutorialManager;
import com.gianlu.pretendyourexyzzy.Utils;

public class GamesFragment extends Fragment implements Pyx.OnResult<GamesList>, GamesAdapter.Listener, SearchView.OnCloseListener, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {
    private static final String POLL_TAG = "games";
    private GamesList lastResult;
    private RecyclerViewLayout recyclerViewLayout;
    private OnParticipateGame handler;
    private SearchView searchView;
    private GamesAdapter adapter;
    private int launchGameGid = -1;
    private String launchGamePassword = null;
    private boolean launchGameShouldRequest;
    private Parcelable recyclerViewSavedInstance;
    private FloatingActionButton createGame;
    private boolean isShowingHint = false;
    private RegisteredPyx pyx;

    public static GamesFragment getInstance(OnParticipateGame handler) {
        GamesFragment fragment = new GamesFragment();
        fragment.setHasOptionsMenu(true);
        fragment.handler = handler;
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
        menu.findItem(R.id.gamesFragment_showLocked).setChecked(!Prefs.getBoolean(getContext(), PKeys.FILTER_LOCKED_LOBBIES, false));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gamesFragment_showLocked:
                boolean show = !item.isChecked();
                item.setChecked(show);
                Prefs.putBoolean(getContext(), PKeys.FILTER_LOCKED_LOBBIES, !show);
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
        if (getContext() == null) return layout;
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        recyclerViewLayout = layout.findViewById(R.id.gamesFragment_recyclerViewLayout);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        createGame = layout.findViewById(R.id.gamesFragment_createGame);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            recyclerViewLayout.showMessage(R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        recyclerViewLayout.enableSwipeRefresh(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pyx.request(PyxRequests.getGamesList(), GamesFragment.this);
            }
        }, R.color.colorAccent);

        createGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtils.showDialog(getActivity(), DialogUtils.progressDialog(getContext(), R.string.loading));
                pyx.request(PyxRequests.createGame(), new Pyx.OnResult<Integer>() {
                    @Override
                    public void onDone(@NonNull Integer result) {
                        DialogUtils.dismissDialog(getActivity());
                        if (handler != null) handler.onParticipatingGame(result);
                    }

                    @Override
                    public void onException(@NonNull Exception ex) {
                        DialogUtils.dismissDialog(getActivity());
                        Toaster.show(getActivity(), Utils.Messages.FAILED_CREATING_GAME, ex);
                    }
                });
            }
        });

        pyx.request(PyxRequests.getGamesList(), this);

        pyx.polling().addListener(POLL_TAG, new Pyx.OnEventListener() {
            @Override
            public void onPollMessage(PollMessage message) {
                if (message.event == PollMessage.Event.GAME_LIST_REFRESH) {
                    recyclerViewSavedInstance = recyclerViewLayout.getList().getLayoutManager().onSaveInstanceState();
                    pyx.request(PyxRequests.getGamesList(), GamesFragment.this);
                }
            }

            @Override
            public void onStoppedPolling() {
            }
        });

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
        recyclerViewLayout.getList().scrollToPosition(0);
    }

    @Override
    public void onDone(@NonNull final GamesList result) {
        if (!isAdded()) return;
        adapter = new GamesAdapter(getContext(), result, pyx, Prefs.getBoolean(getContext(), PKeys.FILTER_LOCKED_LOBBIES, false), this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.getList().getLayoutManager().onRestoreInstanceState(recyclerViewSavedInstance);
        recyclerViewSavedInstance = null;

        lastResult = result;
        updateActivityTitle();
        if (launchGameGid != -1) {
            launchGameInternal(launchGameGid, launchGamePassword, launchGameShouldRequest);
        } else {
            recyclerViewLayout.getList().post(new Runnable() {
                @Override
                public void run() {
                    if (!isShowingHint && TutorialManager.shouldShowHintFor(getContext(), TutorialManager.Discovery.GAMES) && !result.isEmpty() && isVisible())
                        showHints();
                }
            });
        }
    }

    private void showHints() {
        if (getActivity() == null) return;

        scrollToTop();
        GamesAdapter.ViewHolder holder = (GamesAdapter.ViewHolder) recyclerViewLayout.getList().findViewHolderForLayoutPosition(0);
        if (holder != null) {
            isShowingHint = true;
            new TapTargetSequence(getActivity())
                    .target(Utils.tapTargetForView(holder.status, R.string.tutorial_gameStatus, R.string.tutorial_gameStatus_desc))
                    .target(Utils.tapTargetForView(holder.locked, R.string.tutorial_gameLocked, R.string.tutorial_gameLocked_desc))
                    .target(Utils.tapTargetForView(holder.spectate, R.string.tutorial_spectateGame, R.string.tutorial_spectateGame_desc))
                    .target(Utils.tapTargetForView(holder.join, R.string.tutorial_joinGame, R.string.tutorial_joinGame_desc))
                    .target(Utils.tapTargetForView(createGame, R.string.tutorial_createGame, R.string.tutorial_createGame_desc))
                    .listener(new TapTargetSequence.Listener() {
                        @Override
                        public void onSequenceFinish() {
                            TutorialManager.setHintShown(getContext(), TutorialManager.Discovery.GAMES);
                            isShowingHint = false;
                        }

                        @Override
                        public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        }

                        @Override
                        public void onSequenceCanceled(TapTarget lastTarget) {
                        }
                    }).start();
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
        if (isAdded())
            recyclerViewLayout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
    }

    @Nullable
    @Override
    public RecyclerView getRecyclerView() {
        return recyclerViewLayout.getList();
    }

    @Override
    public void spectateGame(final Game game) {
        if (game.hasPassword) {
            askForPassword(new OnPassword() {
                @Override
                public void onPassword(@Nullable String password) {
                    spectateGame(game.gid, password);
                }
            });
        } else {
            spectateGame(game.gid, null);
        }
    }

    @Override
    public void joinGame(final Game game) {
        if (game.hasPassword) {
            askForPassword(new OnPassword() {
                @Override
                public void onPassword(@Nullable String password) {
                    joinGame(game.gid, password);
                }
            });
        } else {
            joinGame(game.gid, null);
        }
    }

    private void spectateGame(final int gid, @Nullable String password) {
        if (getContext() == null) return;

        DialogUtils.showDialog(getActivity(), DialogUtils.progressDialog(getContext(), R.string.loading));
        pyx.request(PyxRequests.spectateGame(gid, password), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                if (handler != null) handler.onParticipatingGame(gid);
                DialogUtils.dismissDialog(getActivity());

                AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_SPECTATE_GAME);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.dismissDialog(getActivity());

                if (ex instanceof PyxException) {
                    switch (((PyxException) ex).errorCode) {
                        case "wp":
                            Toaster.show(getActivity(), Utils.Messages.WRONG_PASSWORD, ex);
                            return;
                        case "gf":
                            Toaster.show(getActivity(), Utils.Messages.GAME_FULL, ex);
                            return;
                    }
                }

                Toaster.show(getActivity(), Utils.Messages.FAILED_SPECTATING, ex);
            }
        });
    }

    private void joinGame(final int gid, @Nullable String password) {
        if (getContext() == null) return;

        DialogUtils.showDialog(getActivity(), DialogUtils.progressDialog(getContext(), R.string.loading));
        pyx.request(PyxRequests.joinGame(gid, password), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                if (handler != null) handler.onParticipatingGame(gid);
                DialogUtils.dismissDialog(getActivity());

                AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_JOIN_GAME);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.dismissDialog(getActivity());

                if (ex instanceof PyxException) {
                    switch (((PyxException) ex).errorCode) {
                        case "wp":
                            Toaster.show(getActivity(), Utils.Messages.WRONG_PASSWORD, ex);
                            return;
                        case "gf":
                            Toaster.show(getActivity(), Utils.Messages.GAME_FULL, ex);
                            return;
                    }
                }

                Toaster.show(getActivity(), Utils.Messages.FAILED_JOINING, ex);
            }
        });
    }

    private void askForPassword(final OnPassword listener) {
        if (getContext() == null) {
            listener.onPassword(null);
            return;
        }

        final EditText password = new EditText(getContext());
        password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.gamePassword)
                .setView(password)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onPassword(password.getText().toString());
                    }
                });

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

    public void launchGame(int gid, @Nullable String password, boolean shouldRequest) {
        if (adapter != null) {
            launchGameInternal(gid, password, shouldRequest);
        } else {
            launchGameShouldRequest = shouldRequest;
            launchGameGid = gid;
            launchGamePassword = password;
        }
    }

    private void launchGameInternal(int gid, @Nullable String password, boolean shouldRequest) {
        Game game = Utils.findGame(adapter.getGames(), gid);
        launchGameGid = -1;
        if (game != null) {
            if (shouldRequest) {
                if (password != null) joinGame(gid, password);
                else joinGame(game);
            } else {
                if (handler != null) handler.onParticipatingGame(gid);
            }
        } else {
            Toaster.show(getActivity(), Utils.Messages.FAILED_JOINING, new NullPointerException("Couldn't find game for " + gid));
        }
    }

    public interface OnParticipateGame {
        void onParticipatingGame(@NonNull Integer gid);
    }

    private interface OnPassword {
        void onPassword(@Nullable String password);
    }
}
