package com.gianlu.pretendyourexyzzy.tutorial;

import android.app.Activity;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.GameLayout;

public class CreateGameTutorial extends BaseTutorial {

    @Keep
    public CreateGameTutorial() {
        super(Discovery.CREATE_GAME);
    }

    public final boolean buildSequence(@NonNull Activity activity, @NonNull GameLayout gameLayout) {
        View options = activity.getWindow().getDecorView().findViewById(R.id.ongoingGame_options);
        if (options != null) {
            forView(options, R.string.tutorial_setupGame, R.string.tutorial_setupGame_desc);
            forView(gameLayout.getStartGameButton(), R.string.startGame, R.string.tutorial_startGame_desc).transparentTarget(true);
            return true;
        }

        return false;
    }
}
