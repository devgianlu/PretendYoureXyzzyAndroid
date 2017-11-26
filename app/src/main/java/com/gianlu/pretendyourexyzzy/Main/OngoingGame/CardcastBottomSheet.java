package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.pretendyourexyzzy.Adapters.CardSetsAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class CardcastBottomSheet extends NiceBaseBottomSheet {
    private final ISheet listener;
    private RecyclerView list;
    private ViewGroup contentParent;

    public CardcastBottomSheet(ViewGroup parent, ISheet listener) {
        super(parent, R.layout.cardcast_sheet_header, R.layout.cardcast_sheet, false);
        this.listener = listener;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onUpdateViews(Object... payloads) {
        updateContentViews((List<CardSet>) payloads[0]);
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        parent.setBackgroundResource(R.color.colorAccent_light);
    }

    private void updateContentViews(List<CardSet> cards) {
        if (cards.isEmpty()) {
            list.setVisibility(View.GONE);
            MessageLayout.show(contentParent, R.string.noCardSets, R.drawable.ic_info_outline_black_48dp);
        } else {
            list.setVisibility(View.VISIBLE);
            MessageLayout.hide(contentParent);
            list.setAdapter(new CardSetsAdapter(getContext(), cards));
        }
    }

    @Override
    protected boolean onPrepareAction(@NonNull FloatingActionButton fab, Object... payloads) {
        if (listener.shouldShowCardcastAdd()) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.addCardcastDeck();
                }
            });

            return true;
        } else {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        contentParent = parent;
        list = parent.findViewById(R.id.cardcastBottomSheet_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        List<CardSet> cards = (List<CardSet>) payloads[0];
        updateContentViews(cards);
    }

    public interface ISheet {
        void addCardcastDeck();

        boolean shouldShowCardcastAdd();
    }
}
