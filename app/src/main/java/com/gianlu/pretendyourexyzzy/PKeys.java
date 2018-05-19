package com.gianlu.pretendyourexyzzy;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.Preferences.Prefs;


public enum PKeys implements Prefs.PrefKey {
    LAST_NICKNAME("lastNickname"),
    FILTER_LOCKED_LOBBIES("filterLockedLobbies"),
    LAST_SERVER("lastServer"),
    USER_SERVERS("userServers"),
    STARRED_CARDS("starredCards"),
    KEEP_SCREEN_ON("keepScreenOn"),
    STARRED_DECKS("starredDecks"),
    LAST_JSESSIONID("lastJSessionId"),
    FIRST_RUN("first_run"),
    TUTORIAL_DISCOVERIES("tutorialDiscoveries"),
    LAST_ID_CODE("lastIdCode");

    private final String key;

    PKeys(String key) {
        this.key = key;
    }

    @NonNull
    @Override
    public String getKey() {
        return key;
    }
}
