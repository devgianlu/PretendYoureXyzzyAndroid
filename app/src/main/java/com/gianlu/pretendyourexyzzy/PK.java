package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.CommonPK;
import com.gianlu.commonutils.Preferences.Prefs;


public class PK extends CommonPK {
    public static final Prefs.Key LAST_NICKNAME = new Prefs.Key("lastNickname");
    public static final Prefs.KeyWithDefault<Boolean> FILTER_LOCKED_LOBBIES = new Prefs.KeyWithDefault<>("filterLockedLobbies", false);
    public static final Prefs.Key LAST_SERVER = new Prefs.Key("lastServer");
    public static final Prefs.Key USER_SERVERS = new Prefs.Key("userServers");
    public static final Prefs.Key API_SERVERS = new Prefs.Key("apiServers");
    public static final Prefs.Key API_SERVERS_CACHE_AGE = new Prefs.Key("apiServersCacheAge");
    public static final Prefs.Key STARRED_CARDS = new Prefs.Key("starredCards");
    public static final Prefs.KeyWithDefault<Boolean> KEEP_SCREEN_ON = new Prefs.KeyWithDefault<>("keepScreenOn", true);
    public static final Prefs.Key STARRED_DECKS = new Prefs.Key("starredDecks");
    public static final Prefs.Key LAST_JSESSIONID = new Prefs.Key("lastJSessionId");
    public static final Prefs.Key LAST_PERSISTENT_ID = new Prefs.Key("lastPid");
    public static final Prefs.Key FIRST_RUN = new Prefs.Key("first_run");
    public static final Prefs.Key LAST_ID_CODE = new Prefs.Key("lastIdCode");
    public static final Prefs.Key WELCOME_MSG_CACHE = new Prefs.Key("welcomeMsgCache");
    public static final Prefs.Key WELCOME_MSG_CACHE_AGE = new Prefs.Key("welcomeMsgCacheAge");
}
