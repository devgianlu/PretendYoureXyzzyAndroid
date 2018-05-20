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
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.CardViews.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.Objects;

public class StarredCardsActivity extends ActivityWithDialog implements CardsAdapter.Listener {
    private RecyclerView list;
    private LinearLayout cards;

    public static void startActivity(Context context) {
        if (StarredCardsManager.hasAnyCard(context))
            context.startActivity(new Intent(context, StarredCardsActivity.class));
        else
            Toaster.show(context, Utils.Messages.NO_STARRED_CARDS);
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
        list.setAdapter(new CardsAdapter(this, false, StarredCardsManager.loadCards(this), GameCardView.Action.DELETE, this));

        cards = findViewById(R.id.starredCards_cards);

        MessageLayout.show((ViewGroup) findViewById(R.id.starredCards_container), R.string.selectAStarredCard, R.drawable.ic_info_outline_black_48dp);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return list;
    }

    private void showCards(@NonNull StarredCardsManager.StarredCard card) {
        MessageLayout.hide((ViewGroup) findViewById(R.id.starredCards_container));

        cards.removeAllViews();
        cards.setTag(card);

        PyxCardsGroupView group = new PyxCardsGroupView(this, card.whiteCards, null, null);
        cards.addView(group);

        GameCardView blackCard = new GameCardView(this, card.blackCard, null, null);
        cards.addView(blackCard, 0);

        group.calcPaddings();
        int[] paddings = group.getPaddings(0, false, null);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) blackCard.getLayoutParams();
        params.setMargins(0, paddings[1], paddings[2], paddings[3]);
    }

    private void deleteCard(@NonNull StarredCardsManager.StarredCard card) {
        StarredCardsManager.removeCard(this, card);
        if (list.getAdapter().getItemCount() == 0) onBackPressed();
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (card instanceof StarredCardsManager.StarredCard) {
            switch (action) {
                case SELECT:
                    showCards((StarredCardsManager.StarredCard) card);
                    break;
                case DELETE:
                    deleteCard((StarredCardsManager.StarredCard) card);
                    if (Objects.equals(cards.getTag(), card)) {
                        MessageLayout.show((ViewGroup) findViewById(R.id.starredCards_container), R.string.selectAStarredCard, R.drawable.ic_info_outline_black_48dp);
                        cards.removeAllViews();
                    }
                    break;
                case TOGGLE_STAR:
                    break;
            }
        }
    }
}
