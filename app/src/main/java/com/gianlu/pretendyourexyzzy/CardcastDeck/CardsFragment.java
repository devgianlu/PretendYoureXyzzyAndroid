package com.gianlu.pretendyourexyzzy.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.cardcastapi.Cardcast;
import com.gianlu.cardcastapi.Models.Card;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.CardcastHelper;
import com.gianlu.pretendyourexyzzy.Cards.FakeCardcastCard;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.R;

import java.util.ArrayList;
import java.util.List;

public class CardsFragment extends Fragment implements CardcastHelper.IResult<List<Card>>, CardsAdapter.IAdapter {
    private RecyclerViewLayout layout;

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
        layout = new RecyclerViewLayout(inflater);
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        layout.disableSwipeRefresh();
        layout.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));

        final boolean whiteCards = getArguments().getBoolean("whiteCards", true);
        final String code = getArguments().getString("code", null);
        if (code == null) {
            layout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        CardcastHelper cardcast = new CardcastHelper(Cardcast.get());
        if (whiteCards) cardcast.getResponses(code, this);
        else cardcast.getCalls(code, this);

        return layout;
    }

    @Override
    public void onDone(List<Card> result) {
        if (!isAdded()) return;

        if (result.isEmpty()) {
            layout.showMessage(R.string.noCards, false);
            return;
        }

        List<FakeCardcastCard> cards = new ArrayList<>();
        for (Card card : result) cards.add(new FakeCardcastCard(card));
        layout.loadListData(new CardsAdapter(getContext(), cards, this));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        if (isAdded())
            layout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return layout.getList();
    }

    @Override
    public void onCardSelected(BaseCard card) {
    }

    @Override
    public void onDeleteCard(StarredCardsManager.StarredCard card) {
    }
}
