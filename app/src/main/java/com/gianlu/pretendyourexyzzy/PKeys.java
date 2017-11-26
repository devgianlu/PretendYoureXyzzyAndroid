package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.Prefs;


public enum PKeys implements Prefs.PrefKey {
    LAST_NICKNAME("lastNickname"),
    FILTER_LOCKED_LOBBIES("filterLockedLobbies"),
    LAST_SERVER("lastServer"),
    STARRED_CARDS("starredCards"),
    KEEP_SCREEN_ON("keepScreenOn"),
    STARRED_DECKS("starredDecks"),
    LAST_JSESSIONID("lastJSessionId"),
    CACHED_DECK_NAMES("cachedDeckNames"),
    CACHED_DECK_CODES("cachedDeckCodes");

    private final String key;

    PKeys(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
