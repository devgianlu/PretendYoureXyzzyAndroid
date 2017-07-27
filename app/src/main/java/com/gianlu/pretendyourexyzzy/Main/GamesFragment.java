package com.gianlu.pretendyourexyzzy.Main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class GamesFragment extends Fragment implements PYX.IResult<GamesList>, GamesAdapter.IAdapter {
    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private FrameLayout layout;

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
    }

    @Override
    public void onException(Exception ex) {
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Nullable
    @Override
    public RecyclerView getRecyclerView() {
        return list;
    }
}
