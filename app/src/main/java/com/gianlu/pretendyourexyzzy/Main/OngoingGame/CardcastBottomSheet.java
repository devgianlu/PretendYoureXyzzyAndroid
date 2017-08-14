package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.support.annotation.NonNull;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardSetsAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

import static android.view.View.GONE;

public class CardcastBottomSheet extends BaseBottomSheet<List<CardSet>> {
    private final ISheet listener;
    private RecyclerView list;
    private Button add;

    public CardcastBottomSheet(View parent, ISheet listener) {
        super(parent, R.layout.cardcast_bottom_sheet, false);
        this.listener = listener;
    }

    @Override
    public void bindViews() {
        title.setText(R.string.cardcast);
        list = content.findViewById(R.id.cardcastBottomSheet_list);
        list.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        add = content.findViewById(R.id.cardcastBottomSheet_add);
    }

    @Override
    protected void setupView(@NonNull List<CardSet> item) {
        add.setVisibility(listener.canEdit() ? View.VISIBLE : GONE);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onAddCardcastDeck();
            }
        });

        if (item.isEmpty()) {
            list.setVisibility(GONE);
            MessageLayout.show(content, R.string.noCardSets, R.drawable.ic_info_outline_black_48dp);
        } else {
            list.setVisibility(View.VISIBLE);
            MessageLayout.hide(content);
            list.setAdapter(new CardSetsAdapter(context, item));
        }
    }

    @Override
    protected void updateView(@NonNull List<CardSet> item) {
        setupView(item);
    }

    public interface ISheet {
        void onAddCardcastDeck();

        boolean canEdit();
    }
}
