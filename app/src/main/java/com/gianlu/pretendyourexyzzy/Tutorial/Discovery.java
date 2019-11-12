package com.gianlu.pretendyourexyzzy.Tutorial;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;

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
