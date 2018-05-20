package com.gianlu.pretendyourexyzzy.SpareActivities.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.CardViews.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class CardsFragment extends Fragment implements Cardcast.OnResult<List<CardcastCard>>, CardsAdapter.Listener {
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = new RecyclerViewLayout(inflater);
        if (getContext() == null) return layout;
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary_background));
        layout.disableSwipeRefresh();
        layout.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));

        layout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int screenWidth = layout.getList().getMeasuredWidth();
                int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 156, getResources().getDisplayMetrics());
                int padding = (screenWidth - cardWidth * 2) / 3;
                layout.getList().setPaddingRelative(padding, 0, 0, 0);
                layout.removeOnLayoutChangeListener(this);
            }
        });

        Bundle args = getArguments();
        String code;
        if (args == null || (code = args.getString("code", null)) == null) {
            layout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        Cardcast cardcast = Cardcast.get();
        if (args.getBoolean("whiteCards", true)) cardcast.getResponses(code, this);
        else cardcast.getCalls(code, this);

        return layout;
    }

    @Override
    public void onDone(@NonNull List<CardcastCard> result) {
        if (!isAdded() || getContext() == null) return;

        if (result.isEmpty()) {
            layout.showMessage(R.string.noCards, false);
            return;
        }

        layout.loadListData(new CardsAdapter(getContext(), true, result, null, this));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        if (isAdded())
            layout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return layout.getList();
    }

    @Override
    public void onCardAction(@NonNull PyxCardsGroupView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
    }
}
