package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.app.ProgressDialog;
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

import com.gianlu.commonutils.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.NetIO.PYXException;
import com.gianlu.pretendyourexyzzy.PKeys;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONException;

public class GamesFragment extends Fragment implements PYX.IResult<GamesList>, GamesAdapter.IAdapter, SearchView.OnCloseListener, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {
    private static final String POLL_TAG = "games";
    private GamesList lastResult;
    private RecyclerViewLayout recyclerViewLayout;
    private IFragment handler;
    private SearchView searchView;
    private GamesAdapter adapter;
    private int launchGameGid = -1;
    private String launchGamePassword = null;
    private boolean launchGameShouldRequest;
    private Parcelable recyclerViewSavedInstance;

    public static GamesFragment getInstance(IFragment handler) {
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
        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        FloatingActionButton createGame = layout.findViewById(R.id.gamesFragment_createGame);

        final PYX pyx = PYX.get(getContext());

        createGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.loading);
                CommonUtils.showDialog(getContext(), pd);
                pyx.createGame(new PYX.IResult<Integer>() {
                    @Override
                    public void onDone(PYX pyx, Integer result) {
                        pyx.getGameInfo(result, new PYX.IResult<GameInfo>() {
                            @Override
                            public void onDone(PYX pyx, GameInfo result) {
                                if (getActivity() != null && !getActivity().isFinishing())
                                    pd.dismiss();

                                if (handler != null) handler.onParticipatingGame(result.game);
                            }

                            @Override
                            public void onException(Exception ex) {
                                if (getActivity() != null && !getActivity().isFinishing())
                                    pd.dismiss();

                                Toaster.show(getActivity(), Utils.Messages.FAILED_JOINING, ex);
                            }
                        });
                    }

                    @Override
                    public void onException(Exception ex) {
                        if (getActivity() != null && !getActivity().isFinishing()) pd.dismiss();
                        Toaster.show(getActivity(), Utils.Messages.FAILED_CREATING_GAME, ex);
                    }
                });
            }
        });

        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pyx.getGamesList(GamesFragment.this);
            }
        });

        pyx.getGamesList(this);

        pyx.getPollingThread().addListener(POLL_TAG, new PYX.IEventListener() {
            @Override
            public void onPollMessage(PollMessage message) throws JSONException {
                if (message.event == PollMessage.Event.GAME_LIST_REFRESH) {
                    recyclerViewSavedInstance = recyclerViewLayout.getList().getLayoutManager().onSaveInstanceState();
                    pyx.getGamesList(GamesFragment.this);
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
        if (adapter != null)
            adapter.setFilterOutLockedLobbies(Prefs.getBoolean(getContext(), PKeys.FILTER_LOCKED_LOBBIES, false));
    }

    public void scrollToTop() {
        recyclerViewLayout.getList().scrollToPosition(0);
    }

    @Override
    public void onDone(PYX pyx, GamesList result) {
        if (!isAdded()) return;
        adapter = new GamesAdapter(getContext(), result, Prefs.getBoolean(getContext(), PKeys.FILTER_LOCKED_LOBBIES, false), this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.getList().getLayoutManager().onRestoreInstanceState(recyclerViewSavedInstance);
        recyclerViewSavedInstance = null;

        lastResult = result;
        updateActivityTitle();

        if (launchGameGid != -1)
            launchGameInternal(launchGameGid, launchGamePassword, launchGameShouldRequest);
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (lastResult != null && activity != null && isVisible())
            activity.setTitle(getString(R.string.games) + " (" + lastResult.size() + "/" + lastResult.maxGames + ") - " + getString(R.string.app_name));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(ex);
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
            askForPassword(new IPassword() {
                @Override
                public void onPassword(@Nullable String password) {
                    spectateGame(game, password);
                }
            });
        } else {
            spectateGame(game, null);
        }
    }

    @Override
    public void joinGame(final Game game) {
        if (game.hasPassword) {
            askForPassword(new IPassword() {
                @Override
                public void onPassword(@Nullable String password) {
                    joinGame(game, password);
                }
            });
        } else {
            joinGame(game, null);
        }
    }

    private void participateGame(int gid) {
        PYX.get(getContext()).getGameInfo(gid, new PYX.IResult<GameInfo>() {
            @Override
            public void onDone(PYX pyx, GameInfo result) {
                if (handler != null) handler.onParticipatingGame(result.game);
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_LOADING, ex);
            }
        });
    }

    private void spectateGame(final Game game, @Nullable String password) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.loading);
        CommonUtils.showDialog(getContext(), pd);
        PYX.get(getContext()).spectateGame(game.gid, password, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                participateGame(game.gid);
                pd.dismiss();

                AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_SPECTATE_GAME);
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();

                if (ex instanceof PYXException) {
                    switch (((PYXException) ex).errorCode) {
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

    private void joinGame(final Game game, @Nullable String password) {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.loading);
        CommonUtils.showDialog(getContext(), pd);
        PYX.get(getContext()).joinGame(game.gid, password, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                participateGame(game.gid);
                if (getActivity() != null && !getActivity().isFinishing()) pd.dismiss();

                AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_JOIN_GAME);
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();

                if (ex instanceof PYXException) {
                    switch (((PYXException) ex).errorCode) {
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

    private void askForPassword(final IPassword listener) {
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

        CommonUtils.showDialog(getActivity(), builder);
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
                if (password != null) joinGame(game, password);
                else joinGame(game);
            } else {
                participateGame(gid);
            }
        }
    }

    public interface IFragment {
        void onParticipatingGame(Game game);
    }

    private interface IPassword {
        void onPassword(@Nullable String password);
    }
}
