package com.gianlu.pretendyourexyzzy.tutorial;

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

import me.toptas.fancyshowcase.FocusShape;

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
        add(forView(blackCard, R.string.tutorial_blackCard)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(8)
                .enableAutoTextPosition()
                .fitSystemWindows(true));

        CardsAdapter.ViewHolder cardHolder = (CardsAdapter.ViewHolder) getFirstVisibleViewHolder(whiteCardsList);
        if (cardHolder != null) {
            GameCardView whiteCard = ((GameCardView) cardHolder.cards.getChildAt(0));
            add(forView(whiteCard, R.string.tutorial_whiteCard)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE)
                    .roundRectRadius(8)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
            add(forView(whiteCard, R.string.tutorial_playCard)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE)
                    .roundRectRadius(8)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
        } else {
            return false;
        }

        PlayersAdapter.ViewHolder playerHolder = (PlayersAdapter.ViewHolder) getFirstVisibleViewHolder(playersList);
        if (playerHolder != null) {
            add(forView(playerHolder.itemView, R.string.tutorial_player)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE)
                    .roundRectRadius(8)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
        } else {
            return false;
        }

        return true;
    }
}
