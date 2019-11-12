package com.gianlu.pretendyourexyzzy.Tutorial;

import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.R;

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

        GamesAdapter.ViewHolder holder = (GamesAdapter.ViewHolder) list.findViewHolderForLayoutPosition(pos);
        if (holder != null) {
            forView(holder.status, R.string.tutorial_gameStatus, R.string.tutorial_gameStatus_desc);
            forView(holder.locked, R.string.tutorial_gameLocked, R.string.tutorial_gameLocked_desc);
            forView(holder.spectate, R.string.tutorial_spectateGame, R.string.tutorial_spectateGame_desc);
            forView(holder.join, R.string.tutorial_joinGame, R.string.tutorial_joinGame_desc);
            forView(createGame, R.string.createGame, R.string.tutorial_createGame_desc)
                    .transparentTarget(true);
            return true;
        }

        return false;
    }
}
