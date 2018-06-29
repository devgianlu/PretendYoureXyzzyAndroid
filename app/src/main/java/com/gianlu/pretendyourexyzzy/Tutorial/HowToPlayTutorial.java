package com.gianlu.pretendyourexyzzy.Tutorial;

import android.content.Context;
import android.graphics.Rect;
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

    public boolean buildSequence(@NonNull Context context, @NonNull TapTargetSequence sequence, @NonNull GameCardView blackCard, @NonNull RecyclerView whiteCardsList, @NonNull RecyclerView playersList) {
        Rect rect = new Rect();
        blackCard.getGlobalVisibleRect(rect);
        sequence.target(TapTarget.forBounds(rect, context.getString(R.string.tutorial_blackCard), context.getString(R.string.tutorial_blackCard_desc))
                .transparentTarget(true));

        CardsAdapter.ViewHolder cardHolder = (CardsAdapter.ViewHolder) getFirstVisibleViewHolder(whiteCardsList);
        if (cardHolder != null) {
            GameCardView whiteCard = ((GameCardView) cardHolder.cards.getChildAt(0));
            sequence.target(TapTarget.forView(whiteCard.text, context.getString(R.string.tutorial_whiteCard), context.getString(R.string.tutorial_whiteCard_desc)));
            sequence.target(TapTarget.forView(whiteCard.primaryAction, context.getString(R.string.tutorial_playCard), context.getString(R.string.tutorial_playCard_desc)));
        } else {
            return false;
        }

        PlayersAdapter.ViewHolder playerHolder = (PlayersAdapter.ViewHolder) getFirstVisibleViewHolder(playersList);
        if (playerHolder != null) {
            sequence.target(TapTarget.forView(playerHolder.itemView, context.getString(R.string.tutorial_player), context.getString(R.string.tutorial_player_desc)));
        } else {
            return false;
        }

        return true;
    }
}
