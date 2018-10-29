package com.gianlu.pretendyourexyzzy.Tutorial;

import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.commonutils.Tutorial.TutorialManager;

import androidx.annotation.NonNull;

public enum Discovery implements TutorialManager.Discovery {
    LOGIN(LoginTutorial.class),
    GAMES(GamesTutorial.class),
    CREATE_GAME(CreateGameTutorial.class),
    HOW_TO_PLAY(HowToPlayTutorial.class);

    private final Class<? extends BaseTutorial> tutorialClass;

    Discovery(@NonNull Class<? extends BaseTutorial> tutorialClass) {
        this.tutorialClass = tutorialClass;
    }

    @NonNull
    @Override
    public Class<? extends BaseTutorial> tutorialClass() {
        return tutorialClass;
    }
}
