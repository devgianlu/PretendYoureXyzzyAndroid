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
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Cards.CardsGroup;
import com.gianlu.pretendyourexyzzy.Cards.PyxCard;
import com.gianlu.pretendyourexyzzy.Cards.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;

import java.util.Objects;

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

        setSupportActionBar((Toolbar) findViewById(R.id.starredCards_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        list = findViewById(R.id.starredCards_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        list.setAdapter(new CardsAdapter(this, StarredCardsManager.loadCards(this), PyxCardsGroupView.Action.DELETE, this));

        cards = findViewById(R.id.starredCards_cards);

        MessageLayout.show((ViewGroup) findViewById(R.id.starredCards_container), R.string.selectAStarredCard, R.drawable.ic_info_outline_black_48dp);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return list;
    }

    private void showCards(StarredCardsManager.StarredCard card) {
        MessageLayout.hide((ViewGroup) findViewById(R.id.starredCards_container));

        cards.removeAllViews();
        cards.setTag(card);

        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        PyxCard blackCard = new PyxCard(this, card.blackCard, null, null);
        cards.addView(blackCard);
        ((LinearLayout.LayoutParams) blackCard.getLayoutParams()).setMargins(margin, margin, margin, margin);

        PyxCardsGroupView groupView = new PyxCardsGroupView(this, card.whiteCards, null, null);
        cards.addView(groupView);
        groupView.setIsFirstOfParent(true);
        groupView.setIsLastOfParent(true);
    }

    private void deleteCard(StarredCardsManager.StarredCard card) {
        StarredCardsManager.removeCard(this, card);
        if (list.getAdapter().getItemCount() == 0) onBackPressed();
    }

    @Override
    public void onCardAction(PyxCardsGroupView.Action action, CardsGroup<? extends BaseCard> group, BaseCard card) {
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
