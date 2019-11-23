package com.gianlu.pretendyourexyzzy.tutorial;

import android.graphics.Rect;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;

public class HowToPlayTutorial extends BaseTutorial {

    @Keep
    public HowToPlayTutorial() {
        super(Discovery.HOW_TO_PLAY);
    }

    @Nullable
    private static RecyclerView.ViewHolder getFirstVisibleViewHolder(@NonNull RecyclerView list) {
        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
        if (llm == null) return null;

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
