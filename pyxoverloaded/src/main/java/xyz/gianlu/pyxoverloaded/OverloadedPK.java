package xyz.gianlu.pyxoverloaded;

import com.gianlu.commonutils.preferences.Prefs;

public abstract class OverloadedPK {
    public static final Prefs.Key STARRED_CARDS_LAST_SYNC = new Prefs.Key("starredCardsLastSync");
    public static final Prefs.Key CUSTOM_DECKS_LAST_SYNC = new Prefs.Key("customDecksLastSync");
    public static final Prefs.Key STARRED_CUSTOM_DECKS_LAST_SYNC = new Prefs.Key("starredCustomDecksLastSync");
}
