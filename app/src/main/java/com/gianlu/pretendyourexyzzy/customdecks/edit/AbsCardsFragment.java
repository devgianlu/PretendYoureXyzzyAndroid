package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;

import java.util.ArrayList;
import java.util.List;

public abstract class AbsCardsFragment extends FragmentWithDialog implements CardsAdapter.Listener {
    protected CustomDecksDatabase db;
    private RecyclerMessageView rmv;
    private Integer id;

    @NonNull
    protected abstract List<? extends BaseCard> getCards(int id);

    @NonNull
    private List<? extends BaseCard> loadCards() {
        if (id == null) return new ArrayList<>(0);
        else return getCards(id);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rmv = new RecyclerMessageView(requireContext());
        rmv.disableSwipeRefresh();
        rmv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rmv.list().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        Integer id = requireArguments().getInt("id", -1);
        if (id == -1) id = null;

        db = CustomDecksDatabase.get(requireContext());
        rmv.loadListData(new CardsAdapter(true, loadCards(), null, null, false, this));
        return rmv;
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        // TODO
    }
}
