package com.gianlu.pretendyourexyzzy.Tutorial;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;

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
        else return list.findViewHolderForLayoutPosition(pos);
    }

    // TODO: Use resources
    public boolean buildSequence(@NonNull TapTargetSequence sequence, @NonNull GameCardView blackCard, @NonNull RecyclerView whiteCardsList, @NonNull RecyclerView playersList) {
        // FIXME: Something is wrong with this, try with the Aria2App solution
        sequence.target(TapTarget.forView(blackCard, "This is the black card", "You have to complete the sentence with the cards below."));

        CardsAdapter.ViewHolder cardHolder = (CardsAdapter.ViewHolder) getFirstVisibleViewHolder(whiteCardsList);
        if (cardHolder != null) {
            GameCardView whiteCard = ((GameCardView) cardHolder.cards.getChildAt(0));
            sequence.target(TapTarget.forView(whiteCard.primaryAction, "Play a card", "You can play a card by tapping this icon."));
            sequence.target(TapTarget.forView(whiteCard.text, "Words definition", "If you don't know the meaning of a word you can select it (usually by pressing and holding) and view its definition."));
        } else {
            return false;
        }

        PlayersAdapter.ViewHolder playerHolder = (PlayersAdapter.ViewHolder) getFirstVisibleViewHolder(playersList);
        if (playerHolder != null) {
            sequence.target(TapTarget.forView(playerHolder.itemView, "This is a player", "Once a player reaches the score goal, it wins the match. You can click on players to view some information."));
        } else {
            return false;
        }

        return true;
    }
}
