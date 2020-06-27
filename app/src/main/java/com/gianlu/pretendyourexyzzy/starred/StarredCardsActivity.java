package com.gianlu.pretendyourexyzzy.starred;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.cards.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.dialogs.CardImageZoomDialog;
import com.gianlu.pretendyourexyzzy.overloaded.SyncUtils;

import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

public class StarredCardsActivity extends ActivityWithDialog implements CardsAdapter.Listener, OverloadedSyncApi.SyncStatusListener {
    private RecyclerView list;
    private LinearLayout cards;
    private MessageView message;
    private TextView syncStatus;

    public static void startActivity(@NonNull Context context) {
        if (StarredCardsDatabase.get(context).hasAnyCard())
            context.startActivity(new Intent(context, StarredCardsActivity.class));
        else
            Toaster.with(context).message(R.string.noStarredCards).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starred_cards);
        setTitle(R.string.starredCards);

        setSupportActionBar(findViewById(R.id.starredCards_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        list = findViewById(R.id.starredCards_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        list.setAdapter(new CardsAdapter(false, StarredCardsDatabase.get(this).getCards(false), GameCardView.Action.SELECT, GameCardView.Action.DELETE, true, this));

        message = findViewById(R.id.starredCards_message);
        cards = findViewById(R.id.starredCards_cards);
        syncStatus = findViewById(R.id.starredCards_sync);

        message.info(R.string.selectAStarredCard);
    }

    @Override
    protected void onStart() {
        super.onStart();
        OverloadedSyncApi.get().addSyncListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OverloadedSyncApi.get().removeSyncListener(this);
    }

    private void showCards(@NonNull StarredCardsDatabase.StarredCard card) {
        message.hide();

        cards.removeAllViews();
        cards.setTag(card);

        PyxCardsGroupView group = new PyxCardsGroupView(this, card.whiteCards, null, null, false, null);
        cards.addView(group);

        GameCardView blackCard = new GameCardView(this, card.blackCard, null, null, null);
        cards.addView(blackCard, 0);

        group.calcPaddings();
        int[] paddings = group.getPaddings(0, false, null);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) blackCard.getLayoutParams();
        params.setMargins(0, paddings[1], paddings[2], paddings[3]);
    }

    private void deleteCard(@NonNull StarredCardsDatabase.StarredCard card) {
        StarredCardsDatabase.get(this).remove(card);
        if (list.getAdapter() != null && list.getAdapter().getItemCount() == 0) onBackPressed();
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (card instanceof StarredCardsDatabase.StarredCard) {
            StarredCardsDatabase.StarredCard starred = (StarredCardsDatabase.StarredCard) card;
            switch (action) {
                case SELECT:
                    showCards(starred);
                    break;
                case DELETE:
                    deleteCard(starred);
                    if (Objects.equals(cards.getTag(), card)) {
                        message.info(R.string.selectAStarredCard);
                        cards.removeAllViews();
                    }
                    break;
                case TOGGLE_STAR:
                    break;
                case SELECT_IMG:
                    showDialog(CardImageZoomDialog.get(card));
                    break;
            }
        }
    }

    @Override
    public void syncStatusUpdated(@NonNull OverloadedSyncApi.SyncProduct product, boolean isSyncing, boolean error) {
        if (syncStatus != null && product == OverloadedSyncApi.SyncProduct.STARRED_CARDS)
            SyncUtils.updateSyncText(syncStatus, product, isSyncing, error);
    }
}
