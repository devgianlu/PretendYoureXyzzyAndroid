package com.gianlu.pretendyourexyzzy;

import android.content.Context;

import com.gianlu.commonutils.Preferences.Prefs;

import java.util.HashSet;
import java.util.Set;

public class TutorialManager {
    public static boolean shouldShowHintFor(Context context, Discovery discovery) {
        if (context == null) return false;
        Set<String> set = Prefs.getSet(context, PKeys.TUTORIAL_DISCOVERIES, new HashSet<String>());
        return !set.contains(discovery.name());
    }

    public static void setHintShown(Context context, Discovery discovery) {
        Prefs.addToSet(context, PKeys.TUTORIAL_DISCOVERIES, discovery.name());
    }

    public static void restartTutorial(Context context) {
        Prefs.remove(context, PKeys.TUTORIAL_DISCOVERIES);
    }

    public enum Discovery {
        LOGIN,
        GAMES,
        CREATE_GAME
    }
}
