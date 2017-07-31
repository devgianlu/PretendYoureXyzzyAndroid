package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
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
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.NetIO.PYXException;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.List;

public class GamesFragment extends Fragment implements PYX.IResult<GamesList>, GamesAdapter.IAdapter, SearchView.OnCloseListener, SearchView.OnQueryTextListener, MenuItemCompat.OnActionExpandListener {
    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private CoordinatorLayout layout;
    private GamesList lastResult;
    private IFragment handler;
    private SearchView searchView;
    private GamesAdapter adapter;
    private int launchGameGid = -1;

    public static GamesFragment getInstance(IFragment handler) {
        GamesFragment fragment = new GamesFragment();
        fragment.setHasOptionsMenu(true);
        fragment.handler = handler;
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.games_fragment, menu);

        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.gamesFragment_search);
        MenuItemCompat.setOnActionExpandListener(item, this);
        searchView = (SearchView) item.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (CoordinatorLayout) inflater.inflate(R.layout.games_fragment, container, false);
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        loading = (ProgressBar) layout.findViewById(R.id.recyclerViewLayout_loading);
        swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent);
        list = (RecyclerView) layout.findViewById(R.id.recyclerViewLayout_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        FloatingActionButton createGame = (FloatingActionButton) layout.findViewById(R.id.gamesFragment_createGame);

        final PYX pyx = PYX.get(getContext());

        createGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pyx.createGame(new PYX.IResult<Integer>() {
                    @Override
                    public void onDone(PYX pyx, Integer result) {
                        pyx.getGameInfo(result, new PYX.IResult<GameInfo>() {
                            @Override
                            public void onDone(PYX pyx, GameInfo result) {
                                if (handler != null) handler.onParticipatingGame(result.game);
                            }

                            @Override
                            public void onException(Exception ex) {
                                Toaster.show(getActivity(), Utils.Messages.FAILED_JOINING, ex);
                            }
                        });
                    }

                    @Override
                    public void onException(Exception ex) {
                        Toaster.show(getActivity(), Utils.Messages.FAILED_CREATING_GAME, ex);
                    }
                });
            }
        });

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pyx.getGamesList(GamesFragment.this);
            }
        });

        pyx.getGamesList(this);

        pyx.pollingThread.addListener(new PYX.IResult<List<PollMessage>>() {
            @Override
            public void onDone(PYX pyx, List<PollMessage> result) {
                for (PollMessage message : result)
                    if (message.event == PollMessage.Event.GAME_LIST_REFRESH)
                        pyx.getGamesList(GamesFragment.this);
            }

            @Override
            public void onException(Exception ex) {
                Logging.logMe(getContext(), ex);
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
        if (list != null) list.scrollToPosition(0);
    }

    @Override
    public void onDone(PYX pyx, GamesList result) {
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        MessageLayout.hide(layout);

        adapter = new GamesAdapter(getContext(), result, this);
        list.setAdapter(adapter);
        lastResult = result;
        updateActivityTitle();

        if (launchGameGid != -1) launchGameInternal(launchGameGid);
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (lastResult != null && activity != null && isVisible())
            activity.setTitle(getString(R.string.games) + " (" + lastResult.size() + "/" + lastResult.maxGames + ") - " + getString(R.string.app_name));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Nullable
    @Override
    public RecyclerView getRecyclerView() {
        return list;
    }

    @Override
    public void spectateGame(final Game game) {
        if (game.hasPassword) {
            askForPassword(new IPassword() {
                @Override
                public void onPassword(String password) {
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
                public void onPassword(String password) {
                    joinGame(game, password);
                }
            });
        } else {
            joinGame(game, null);
        }
    }

    private void spectateGame(final Game game, @Nullable String password) {
        PYX.get(getContext()).spectateGame(game.gid, password, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                if (handler != null) handler.onParticipatingGame(game);
            }

            @Override
            public void onException(Exception ex) {
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
        PYX.get(getContext()).joinGame(game.gid, password, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                if (handler != null) handler.onParticipatingGame(game);
            }

            @Override
            public void onException(Exception ex) {
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

    public void launchGame(int gid) {
        if (adapter != null) launchGameInternal(gid);
        else launchGameGid = gid;
    }

    private void launchGameInternal(int gid) {
        Game game = Utils.findGame(adapter.getGames(), gid);
        launchGameGid = -1;
        if (game != null) joinGame(game);
    }

    public interface IFragment {
        void onParticipatingGame(Game game);
    }

    private interface IPassword {
        void onPassword(String password);
    }
}
