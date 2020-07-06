package com.gianlu.pretendyourexyzzy.tutorial;

import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.main.GamesFragment;

import me.toptas.fancyshowcase.FocusShape;

public class GamesTutorial extends BaseTutorial {

    @Keep
    public GamesTutorial() {
        super(Discovery.GAMES);
    }

    public final boolean buildSequence(@NonNull View createGame, @NonNull RecyclerView list) {
        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
        if (llm == null) return false;

        int pos = llm.findFirstVisibleItemPosition();
        if (pos == -1) return false;

        GamesFragment.GamesAdapter.ViewHolder holder = (GamesFragment.GamesAdapter.ViewHolder) list.findViewHolderForLayoutPosition(pos);
        if (holder != null) {
            add(forView(holder.status, R.string.tutorial_gameStatus)
                    .focusShape(FocusShape.CIRCLE)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
            add(forView(holder.locked, R.string.tutorial_gameLocked)
                    .focusShape(FocusShape.CIRCLE)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
            add(forView(holder.spectate, R.string.tutorial_spectateGame)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE)
                    .roundRectRadius(8)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
            add(forView(holder.join, R.string.tutorial_joinGame)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE)
                    .roundRectRadius(8)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
            add(forView(createGame, R.string.tutorial_createGame)
                    .focusShape(FocusShape.CIRCLE)
                    .enableAutoTextPosition()
                    .fitSystemWindows(true));
            return true;
        }

        return false;
    }
}
