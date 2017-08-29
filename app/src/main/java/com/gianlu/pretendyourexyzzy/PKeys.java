package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.Prefs;


public enum PKeys implements Prefs.PrefKey {
    LAST_NICKNAME("lastNickname"),
    LAST_SERVER("lastServer"),
    STARRED_CARDS("starredCards"),
    KEEP_SCREEN_ON("keepScreenOn");

    private final String key;

    PKeys(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
