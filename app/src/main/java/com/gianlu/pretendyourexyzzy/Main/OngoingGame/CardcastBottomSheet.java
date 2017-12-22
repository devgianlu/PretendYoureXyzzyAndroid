package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.pretendyourexyzzy.Adapters.CardSetsAdapter;
import com.gianlu.pretendyourexyzzy.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class CardcastBottomSheet extends NiceBaseBottomSheet {
    private final int gid;
    private final CardcastDeckActivity.IOngoingGame listener;
    private RecyclerView list;
    private ViewGroup contentParent;

    public CardcastBottomSheet(ViewGroup parent, int gid, CardcastDeckActivity.IOngoingGame listener) {
        super(parent, R.layout.sheet_header_cardcast, R.layout.sheet_cardcast, false);
        this.gid = gid;
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
            list.setAdapter(new CardSetsAdapter(getContext(), gid, cards, listener));
        }
    }

    @Override
    protected boolean onPrepareAction(@NonNull FloatingActionButton fab, Object... payloads) {
        if (listener.canModifyCardcastDecks()) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddCardcastDeckDialog();
                }
            });

            return true;
        } else {
            return false;
        }
    }

    private void showAddCardcastDeckDialog() {
        final EditText code = new EditText(getContext());
        code.setHint("XXXXX");
        code.setAllCaps(true);
        code.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5), new InputFilter.AllCaps()});

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.addCardcast)
                .setView(code)
                .setNeutralButton(R.string.addStarred, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) listener.addCardcastStarredDecks();
                    }
                })
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (listener != null) listener.addCardcastDeck(code.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        CommonUtils.showDialog(getContext(), builder);
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
}
