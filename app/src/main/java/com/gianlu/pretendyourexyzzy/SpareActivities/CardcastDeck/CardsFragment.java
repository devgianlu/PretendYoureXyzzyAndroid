package com.gianlu.pretendyourexyzzy.SpareActivities.CardcastDeck;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.Dialogs.CardImageZoomDialog;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class CardsFragment extends Fragment implements Cardcast.OnResult<List<CardcastCard>>, CardsAdapter.Listener {
    private RecyclerViewLayout layout;

    @NonNull
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
        layout = new RecyclerViewLayout(requireContext());
        layout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary_background));
        layout.disableSwipeRefresh();
        layout.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        layout.getList().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        Bundle args = getArguments();
        String code;
        if (args == null || (code = args.getString("code", null)) == null) {
            layout.showError(R.string.failedLoading);
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
            layout.showInfo(R.string.noCards, false);
            return;
        }

        layout.loadListData(new CardsAdapter(getContext(), true, result, null, null, false, this));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        layout.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return layout.getList();
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (action == GameCardView.Action.SELECT_IMG)
            DialogUtils.showDialog(getActivity(), CardImageZoomDialog.get(card));
    }
}
