package com.gianlu.pretendyourexyzzy.Starred;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;

import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.MessageView;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.CardViews.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.Dialogs.CardImageZoomDialog;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Objects;

public class StarredCardsActivity extends ActivityWithDialog implements CardsAdapter.Listener {
    private RecyclerView list;
    private LinearLayout cards;
    private MessageView message;

    public static void startActivity(@NonNull Context context) {
        if (StarredCardsManager.hasAnyCard())
            context.startActivity(new Intent(context, StarredCardsActivity.class));
        else
            Toaster.with(context).message(R.string.noStarredCards).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starred_cards);
        setTitle(R.string.starredCards);

        setSupportActionBar((Toolbar) findViewById(R.id.starredCards_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        list = findViewById(R.id.starredCards_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        list.setAdapter(new CardsAdapter(this, false, StarredCardsManager.loadCards(), GameCardView.Action.SELECT, GameCardView.Action.DELETE, true, this));

        message = findViewById(R.id.starredCards_message);
        cards = findViewById(R.id.starredCards_cards);

        message.setInfo(R.string.selectAStarredCard);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return list;
    }

    private void showCards(@NonNull StarredCardsManager.StarredCard card) {
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

    private void deleteCard(@NonNull StarredCardsManager.StarredCard card) {
        StarredCardsManager.removeCard(card);
        if (list.getAdapter().getItemCount() == 0) onBackPressed();
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (card instanceof StarredCardsManager.StarredCard) {
            StarredCardsManager.StarredCard starred = (StarredCardsManager.StarredCard) card;
            switch (action) {
                case SELECT:
                    showCards(starred);
                    break;
                case DELETE:
                    deleteCard(starred);
                    if (Objects.equals(cards.getTag(), card)) {
                        message.setInfo(R.string.selectAStarredCard);
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
}
