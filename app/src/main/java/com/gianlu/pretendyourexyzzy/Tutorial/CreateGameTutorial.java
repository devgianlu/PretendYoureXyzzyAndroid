package com.gianlu.pretendyourexyzzy.Tutorial;

import android.app.Activity;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.pretendyourexyzzy.Main.OngoingGame.BestGameManager;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class CreateGameTutorial extends BaseTutorial {

    @Keep
    public CreateGameTutorial() {
        super(Discovery.CREATE_GAME);
    }

    public final boolean buildSequence(@NonNull Activity activity, @NonNull TapTargetSequence sequence, @NonNull BestGameManager gameManager) {
        View options = activity.getWindow().getDecorView().findViewById(R.id.ongoingGame_options);
        if (options != null) {
            sequence.target(Utils.tapTargetForView(options, R.string.tutorial_setupGame, R.string.tutorial_setupGame_desc))
                    .target(Utils.tapTargetForView(gameManager.getStartGameButton(), R.string.tutorial_startGame, R.string.tutorial_startGame_desc));

            return true;
        }

        return false;
    }
}
