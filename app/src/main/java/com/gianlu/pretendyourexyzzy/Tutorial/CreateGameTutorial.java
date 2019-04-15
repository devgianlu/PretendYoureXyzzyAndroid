package com.gianlu.pretendyourexyzzy.Tutorial;

import android.app.Activity;
import android.view.View;

import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.Main.OngoingGame.GameLayout;
import com.gianlu.pretendyourexyzzy.R;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

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
