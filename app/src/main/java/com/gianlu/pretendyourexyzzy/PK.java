package com.gianlu.pretendyourexyzzy;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.Preferences.Prefs;


public enum PK implements Prefs.PrefKey {
    LAST_NICKNAME("lastNickname"),
    FILTER_LOCKED_LOBBIES("filterLockedLobbies"),
    LAST_SERVER("lastServer"),
    USER_SERVERS("userServers"),
    STARRED_CARDS("starredCards"),
    KEEP_SCREEN_ON("keepScreenOn"),
    STARRED_DECKS("starredDecks"),
    LAST_JSESSIONID("lastJSessionId"),
    LAST_PERSISTENT_ID("lastPid"),
    FIRST_RUN("first_run"),
    LAST_ID_CODE("lastIdCode"),
    WELCOME_MSG_CACHE("welcomeMsgCache"),
    WELCOME_MSG_CACHE_AGE("welcomeMsgCacheAge");

    private final String key;

    PK(String key) {
        this.key = key;
    }

    @NonNull
    @Override
    public String getKey() {
        return key;
    }
}
