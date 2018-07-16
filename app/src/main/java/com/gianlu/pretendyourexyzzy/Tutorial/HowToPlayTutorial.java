package com.gianlu.pretendyourexyzzy.Tutorial;

import android.graphics.Rect;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.R;

public class HowToPlayTutorial extends BaseTutorial {

    @Keep
    public HowToPlayTutorial() {
        super(Discovery.HOW_TO_PLAY);
    }

    @Nullable
    private static RecyclerView.ViewHolder getFirstVisibleViewHolder(@NonNull RecyclerView list) {
        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
        int pos = llm.findFirstCompletelyVisibleItemPosition();
        if (pos == -1) return null;
        else return list.findViewHolderForAdapterPosition(pos);
    }

    public boolean buildSequence(@NonNull GameCardView blackCard, @NonNull RecyclerView whiteCardsList, @NonNull RecyclerView playersList) {
        Rect rect = new Rect();
        blackCard.getGlobalVisibleRect(rect);
        forBounds(rect, R.string.tutorial_blackCard, R.string.tutorial_blackCard_desc)
                .transparentTarget(true);

        CardsAdapter.ViewHolder cardHolder = (CardsAdapter.ViewHolder) getFirstVisibleViewHolder(whiteCardsList);
        if (cardHolder != null) {
            GameCardView whiteCard = ((GameCardView) cardHolder.cards.getChildAt(0));
            forView(whiteCard.text, R.string.tutorial_whiteCard, R.string.tutorial_whiteCard_desc);
            forView(whiteCard.primaryAction, R.string.tutorial_playCard, R.string.tutorial_playCard_desc);
        } else {
            return false;
        }

        PlayersAdapter.ViewHolder playerHolder = (PlayersAdapter.ViewHolder) getFirstVisibleViewHolder(playersList);
        if (playerHolder != null) {
            forView(playerHolder.itemView, R.string.tutorial_player, R.string.tutorial_player_desc);
        } else {
            return false;
        }

        return true;
    }
}
