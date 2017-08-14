package com.gianlu.pretendyourexyzzy.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.gianlu.cardcastapi.Cardcast;
import com.gianlu.cardcastapi.Models.Card;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.CardcastHelper;
import com.gianlu.pretendyourexyzzy.Cards.FakeCardcastCard;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.R;

import java.util.ArrayList;
import java.util.List;

public class CardsFragment extends Fragment implements CardcastHelper.IResult<List<Card>>, CardsAdapter.IAdapter {
    private FrameLayout layout;
    private ProgressBar loading;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView list;

    public static CardsFragment getInstance(Context context, boolean whiteCards, String code) {
        CardsFragment fragment = new CardsFragment();
        Bundle args = new Bundle();
        args.putString("code", code);
        args.putBoolean("whiteCards", whiteCards);
        args.putString("title", context.getString(whiteCards ? R.string.whiteCards : R.string.blackCards));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.recycler_view_layout, container, false);
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        loading = layout.findViewById(R.id.recyclerViewLayout_loading);
        swipeRefresh = layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setEnabled(false);
        list = layout.findViewById(R.id.recyclerViewLayout_list);
        list.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));

        final boolean whiteCards = getArguments().getBoolean("whiteCards", true);
        final String code = getArguments().getString("code", null);
        if (code == null) {
            loading.setVisibility(View.GONE);
            swipeRefresh.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        CardcastHelper cardcast = new CardcastHelper(getContext(), Cardcast.get());
        if (whiteCards) cardcast.getResponses(code, this);
        else cardcast.getCalls(code, this);

        return layout;
    }

    @Override
    public void onDone(List<Card> result) {
        if (result.isEmpty()) {
            loading.setVisibility(View.GONE);
            swipeRefresh.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.noCards, R.drawable.ic_info_outline_black_48dp);
            return;
        }

        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        MessageLayout.hide(layout);

        List<FakeCardcastCard> cards = new ArrayList<>();
        for (Card card : result) cards.add(new FakeCardcastCard(card));
        list.setAdapter(new CardsAdapter(getContext(), cards, this));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return list;
    }

    @Override
    public void onCardSelected(BaseCard card) {
    }

    @Override
    public void onDeleteCard(StarredCardsManager.StarredCard card) {
    }
}
