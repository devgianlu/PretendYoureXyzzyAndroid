package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.NetIO.PYXException;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.List;

public class GamesFragment extends Fragment implements PYX.IResult<GamesList>, GamesAdapter.IAdapter {
    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private FrameLayout layout;
    private GamesList lastResult;

    public static GamesFragment getInstance() {
        return new GamesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.recycler_view_layout, container, false);
        loading = (ProgressBar) layout.findViewById(R.id.recyclerViewLayout_loading);
        swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent);
        list = (RecyclerView) layout.findViewById(R.id.recyclerViewLayout_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        final PYX pyx = PYX.get(getContext());

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

        list.setAdapter(new GamesAdapter(getContext(), result, this));
        lastResult = result;
        updateActivityTitle();
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (lastResult != null && activity != null && isVisible())
            activity.setTitle(getString(R.string.games) + " (" + lastResult.size() + "/" + lastResult.maxGames + ") - " + getString(R.string.app_name));
    }

    @Override
    public void onException(Exception ex) {
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        if (!isDetached())
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

    private void spectateGame(Game game, @Nullable String password) {
        PYX.get(getContext()).spectateGame(game.gid, password, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                // TODO: Spectate
            }

            @Override
            public void onException(Exception ex) {
                if (ex instanceof PYXException) {
                    switch (ex.getMessage()) {
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

    private void joinGame(Game game, @Nullable String password) {
        PYX.get(getContext()).joinGame(game.gid, password, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                // TODO: Join
            }

            @Override
            public void onException(Exception ex) {
                if (ex instanceof PYXException) {
                    switch (ex.getMessage()) {
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

    private interface IPassword {
        void onPassword(String password);
    }
}
