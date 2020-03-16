package com.gianlu.pretendyourexyzzy.activities.cardcastdeck;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.Cardcast;
import com.gianlu.pretendyourexyzzy.api.models.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.CardcastCard;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.dialogs.CardImageZoomDialog;

import java.util.List;

public class CardsFragment extends FragmentWithDialog implements Cardcast.OnResult<List<CardcastCard>>, CardsAdapter.Listener {
    private static final String TAG = CardsFragment.class.getSimpleName();
    private RecyclerMessageView rmv;

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
        rmv = new RecyclerMessageView(requireContext());
        rmv.disableSwipeRefresh();
        rmv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rmv.list().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        Bundle args = getArguments();
        String code;
        if (args == null || (code = args.getString("code", null)) == null) {
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        Cardcast cardcast = Cardcast.get();
        if (args.getBoolean("whiteCards", true)) cardcast.getResponses(code, null, this);
        else cardcast.getCalls(code, null, this);

        return rmv;
    }

    @Override
    public void onDone(@NonNull List<CardcastCard> result) {
        if (result.isEmpty()) {
            rmv.showInfo(R.string.noCards, false);
            return;
        }

        rmv.loadListData(new CardsAdapter(true, result, null, null, false, this));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading cards.", ex);
        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return rmv.list();
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (action == GameCardView.Action.SELECT_IMG)
            DialogUtils.showDialog(getActivity(), CardImageZoomDialog.get(card), null);
    }
}
