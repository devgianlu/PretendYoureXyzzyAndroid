package com.gianlu.pretendyourexyzzy.Main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.gianlu.cardcastapi.Cardcast;
import com.gianlu.cardcastapi.Models.Deck;
import com.gianlu.cardcastapi.Models.Decks;
import com.gianlu.commonutils.InfiniteRecyclerView;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardcastDecksAdapter;
import com.gianlu.pretendyourexyzzy.CardcastHelper;
import com.gianlu.pretendyourexyzzy.R;

public class CardcastFragment extends Fragment implements CardcastHelper.IDecks, CardcastDecksAdapter.IAdapter {
    private final static int LIMIT = 12;
    private FrameLayout layout;
    private InfiniteRecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private CardcastHelper cardcast;

    public static CardcastFragment getInstance() {
        return new CardcastFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.recycler_view_layout, container, false);
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        loading = layout.findViewById(R.id.recyclerViewLayout_loading);
        swipeRefresh = layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent);
        list = layout.findViewById(R.id.recyclerViewLayout_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        cardcast = new CardcastHelper(getContext(), Cardcast.get());
        final CardcastHelper.Search search = new CardcastHelper.Search(null, null, Cardcast.Direction.DESCENDANT, Cardcast.Sort.RATING, true);
        cardcast.getDecks(search, LIMIT, 0, this);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                cardcast.getDecks(search, LIMIT, 0, CardcastFragment.this);
            }
        });

        return layout;
    }

    @Override
    public void onDone(CardcastHelper.Search search, Decks decks) {
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        MessageLayout.hide(layout);

        list.setAdapter(new CardcastDecksAdapter(getContext(), cardcast, search, decks, LIMIT, this));
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

    @Override
    public void onDeckSelected(Deck deck) { // TODO

    }
}
