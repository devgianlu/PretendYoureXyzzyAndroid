package com.gianlu.pretendyourexyzzy.Cards;

import android.content.Context;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PKeys;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class StarredDecksManager {

    public static boolean hasDeck(Context context, String code) {
        try {
            JSONArray starredDecksArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_DECKS, "[]"));
            List<StarredDeck> starredDecks = CommonUtils.toTList(starredDecksArray, StarredDeck.class);
            for (StarredDeck deck : starredDecks)
                if (Objects.equals(deck.code, code))
                    return true;

            return false;
        } catch (JSONException ex) {
            Logging.logMe(ex);
            return false;
        }
    }

    public static void addDeck(Context context, StarredDeck deck) {
        try {
            JSONArray starredDecksArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_DECKS, "[]"));
            List<StarredDeck> starredDecks = CommonUtils.toTList(starredDecksArray, StarredDeck.class);
            if (!starredDecks.contains(deck)) starredDecks.add(deck);
            saveDecks(context, starredDecks);
        } catch (JSONException ex) {
            Logging.logMe(ex);
        }

        AnalyticsApplication.sendAnalytics(context, Utils.ACTION_STARRED_DECK_ADD);
    }

    private static void saveDecks(Context context, List<StarredDeck> decks) {
        try {
            JSONArray starredDecksArray = new JSONArray();
            for (StarredDeck deck : decks) starredDecksArray.put(deck.toJSON());
            Prefs.putBase64String(context, PKeys.STARRED_DECKS, starredDecksArray.toString());
        } catch (JSONException ex) {
            Logging.logMe(ex);
        }
    }

    public static void removeDeck(Context context, String code) {
        try {
            JSONArray starredDecksArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_DECKS, "[]"));
            List<StarredDeck> starredDecks = CommonUtils.toTList(starredDecksArray, StarredDeck.class);
            Iterator<StarredDeck> iterator = starredDecks.iterator();
            while (iterator.hasNext())
                if (Objects.equals(iterator.next().code, code))
                    iterator.remove();

            saveDecks(context, starredDecks);
        } catch (JSONException ex) {
            Logging.logMe(ex);
        }
    }

    public static List<StarredDeck> loadDecks(Context context) {
        try {
            JSONArray starredDecksArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_DECKS, "[]"));
            List<StarredDeck> decks = CommonUtils.toTList(starredDecksArray, StarredDeck.class);
            Collections.reverse(decks);
            return decks;
        } catch (JSONException ex) {
            Logging.logMe(ex);
            return new ArrayList<>();
        }
    }

    public static boolean hasAnyDeck(Context context) {
        try {
            return new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_DECKS, "[]")).length() > 0;
        } catch (JSONException ex) {
            Logging.logMe(ex);
            return false;
        }
    }

    public static class StarredDeck {
        public final String code;
        public final String name;

        public StarredDeck(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public StarredDeck(JSONObject obj) throws JSONException {
            code = obj.getString("code");
            name = obj.getString("name");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredDeck that = (StarredDeck) o;
            return code.equals(that.code) && name.equals(that.name);
        }

        public JSONObject toJSON() throws JSONException {
            return new JSONObject()
                    .put("code", code)
                    .put("name", name);
        }
    }
}
