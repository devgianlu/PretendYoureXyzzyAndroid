package com.gianlu.pretendyourexyzzy.Tutorial;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.Adapters.GamesAdapter;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class GamesTutorial extends BaseTutorial {

    @Keep
    public GamesTutorial() {
        super(Discovery.GAMES);
    }

    public final boolean buildSequence(@NonNull TapTargetSequence sequence, @NonNull View createGame, @NonNull RecyclerView list) {
        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
        int pos = llm.findFirstVisibleItemPosition();
        if (pos == -1) return false;

        GamesAdapter.ViewHolder holder = (GamesAdapter.ViewHolder) list.findViewHolderForLayoutPosition(pos);
        if (holder != null) {
            sequence.target(Utils.tapTargetForView(holder.status, R.string.tutorial_gameStatus, R.string.tutorial_gameStatus_desc))
                    .target(Utils.tapTargetForView(holder.locked, R.string.tutorial_gameLocked, R.string.tutorial_gameLocked_desc))
                    .target(Utils.tapTargetForView(holder.spectate, R.string.tutorial_spectateGame, R.string.tutorial_spectateGame_desc))
                    .target(Utils.tapTargetForView(holder.join, R.string.tutorial_joinGame, R.string.tutorial_joinGame_desc))
                    .target(Utils.tapTargetForView(createGame, R.string.tutorial_createGame, R.string.tutorial_createGame_desc));
            return true;
        }

        return false;
    }
}
