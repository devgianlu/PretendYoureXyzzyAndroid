package com.gianlu.pretendyourexyzzy.tutorial;

import android.app.Activity;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.GameLayout;

import me.toptas.fancyshowcase.FocusShape;

public class CreateGameTutorial extends BaseTutorial {

    @Keep
    public CreateGameTutorial() {
        super(Discovery.CREATE_GAME);
    }

    public final boolean buildSequence(@NonNull Activity activity, @NonNull GameLayout gameLayout) {
        View options = activity.getWindow().getDecorView().findViewById(R.id.ongoingGame_options);
        if (options != null) {
            add(forView(options, R.string.tutorial_setupGame)
                    .enableAutoTextPosition()
                    .focusShape(FocusShape.CIRCLE)
                    .fitSystemWindows(true));
            add(forView(gameLayout.getStartGameButton(), R.string.tutorial_startGame)
                    .enableAutoTextPosition()
                    .focusShape(FocusShape.CIRCLE)
                    .fitSystemWindows(true));
            return true;
        }

        return false;
    }
}
