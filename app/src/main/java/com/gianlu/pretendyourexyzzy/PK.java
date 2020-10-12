package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.preferences.CommonPK;
import com.gianlu.commonutils.preferences.Prefs;


public final class PK extends CommonPK {
    public static final Prefs.Key LAST_NICKNAME = new Prefs.Key("lastNickname");
    public static final Prefs.KeyWithDefault<Boolean> FILTER_LOCKED_LOBBIES = new Prefs.KeyWithDefault<>("filterLockedLobbies", false);
    public static final Prefs.KeyWithDefault<String> FILTER_GAME_STATUS = new Prefs.KeyWithDefault<>("filterGameStatus", "any");
    public static final Prefs.Key LAST_SERVER = new Prefs.Key("lastServer");
    public static final Prefs.Key USER_SERVERS = new Prefs.Key("userServers");
    public static final Prefs.Key API_SERVERS = new Prefs.Key("apiServers");
    public static final Prefs.Key API_SERVERS_CACHE_AGE = new Prefs.Key("apiServersCacheAge");
    public static final Prefs.KeyWithDefault<Boolean> KEEP_SCREEN_ON = new Prefs.KeyWithDefault<>("keepScreenOn", true);
    public static final Prefs.Key LAST_JSESSIONID = new Prefs.Key("lastJSessionId");
    public static final Prefs.Key LAST_PERSISTENT_ID = new Prefs.Key("lastPid");
    public static final Prefs.Key FIRST_RUN = new Prefs.Key("first_run");
    public static final Prefs.Key LAST_ID_CODE = new Prefs.Key("lastIdCode");
    public static final Prefs.Key WELCOME_MSG_CACHE = new Prefs.Key("welcomeMsgCache");
    public static final Prefs.Key WELCOME_MSG_CACHE_AGE = new Prefs.Key("welcomeMsgCacheAge");
    public static final Prefs.Key BLOCKED_USERS = new Prefs.Key("blockedUsers");
    public static final Prefs.Key OVERLOADED_LAST_ENABLED = new Prefs.Key("overloadedLastEnabled");
    public static final Prefs.Key STARRED_CARDS_REVISION = new Prefs.Key("starredCardsRevision");
    public static final Prefs.Key STARRED_CUSTOM_DECKS_REVISION = new Prefs.Key("starredCustomDecksRevision");
    public static final Prefs.Key LAST_CHANGELOG_SHOWN = new Prefs.Key("lastChangelogShown");
    public static final Prefs.Key CR_CAST_TOKEN = new Prefs.Key("crCastToken");
    public static final Prefs.Key CR_CAST_USER = new Prefs.Key("crCastUser");
    public static final Prefs.Key CR_CAST_PASSWORD = new Prefs.Key("crCastPass");
    @Deprecated
    public static final Prefs.Key STARRED_CARDS = new Prefs.Key("starredCards");
    public static final Prefs.KeyWithDefault<Boolean> ONE_TIME_LOGIN_SHOWN = new Prefs.KeyWithDefault<>("oneTimeLoginShown", false);
}
