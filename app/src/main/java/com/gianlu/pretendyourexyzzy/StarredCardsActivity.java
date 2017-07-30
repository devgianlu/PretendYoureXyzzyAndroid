package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Cards.CardGroupView;
import com.gianlu.pretendyourexyzzy.Cards.PyxCard;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;

public class StarredCardsActivity extends AppCompatActivity implements CardsAdapter.IAdapter {
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.starredCards_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        list = (RecyclerView) findViewById(R.id.starredCards_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        list.setAdapter(new CardsAdapter(this, StarredCardsManager.loadCards(this), this));

        cards = (LinearLayout) findViewById(R.id.starredCards_cards);

        MessageLayout.show((ViewGroup) findViewById(R.id.starredCards_container), R.string.selectAStarredCard, R.drawable.ic_info_outline_black_48dp);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return list;
    }

    @Override
    public void onCardSelected(BaseCard card) {
        MessageLayout.hide((ViewGroup) findViewById(R.id.starredCards_container));
        StarredCardsManager.StarredCard starredCard = ((StarredCardsManager.StarredCard) card);

        cards.removeAllViews();
        cards.addView(new PyxCard(this, starredCard.blackCard, false, null, null));
        cards.addView(new CardGroupView(this, starredCard.whiteCards, null));
    }

    @Override
    public void onDeleteCard(StarredCardsManager.StarredCard card) {
        StarredCardsManager.removeCard(this, card);
        if (list.getAdapter().getItemCount() == 0) onBackPressed();
    }
}
