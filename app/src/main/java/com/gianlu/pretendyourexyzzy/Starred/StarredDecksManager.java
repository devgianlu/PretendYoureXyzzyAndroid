package com.gianlu.pretendyourexyzzy.Starred;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class StarredDecksManager {

    public static boolean hasDeck(@NonNull String code) {
        try {
            List<StarredDeck> decks = StarredDeck.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_DECKS, "[]")));
            for (StarredDeck deck : decks)
                if (Objects.equals(deck.code, code))
                    return true;

            return false;
        } catch (JSONException ex) {
            Logging.log(ex);
            return false;
        }
    }

    public static void addDeck(@NonNull StarredDeck deck) {
        try {
            List<StarredDeck> decks = StarredDeck.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_DECKS, "[]")));
            if (!decks.contains(deck)) decks.add(deck);
            saveDecks(decks);
        } catch (JSONException ex) {
            Logging.log(ex);
        }

        // TODO: AnalyticsApplication.sendAnalytics( Utils.ACTION_STARRED_DECK_ADD);
    }

    private static void saveDecks(List<StarredDeck> decks) {
        try {
            JSONArray array = new JSONArray();
            for (StarredDeck deck : decks) array.put(deck.toJson());
            Prefs.putBase64String(PK.STARRED_DECKS, array.toString());
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    public static void removeDeck(@NonNull String code) {
        try {
            List<StarredDeck> decks = StarredDeck.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_DECKS, "[]")));
            Iterator<StarredDeck> iterator = decks.iterator();
            while (iterator.hasNext())
                if (Objects.equals(iterator.next().code, code))
                    iterator.remove();

            saveDecks(decks);
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    public static List<StarredDeck> loadDecks() {
        try {
            List<StarredDeck> decks = StarredDeck.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_DECKS, "[]")));
            Collections.reverse(decks);
            return decks;
        } catch (JSONException ex) {
            Logging.log(ex);
            return new ArrayList<>();
        }
    }

    public static boolean hasAnyDeck() {
        try {
            return new JSONArray(Prefs.getBase64String(PK.STARRED_DECKS, "[]")).length() > 0;
        } catch (JSONException ex) {
            Logging.log(ex);
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

        private StarredDeck(JSONObject obj) throws JSONException {
            code = obj.getString("code");
            name = obj.getString("name");
        }

        @NonNull
        private static List<StarredDeck> asList(JSONArray array) throws JSONException {
            List<StarredDeck> decks = new ArrayList<>();
            for (int i = 0; i < array.length(); i++)
                decks.add(new StarredDeck(array.getJSONObject(i)));
            return decks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredDeck that = (StarredDeck) o;
            return code.equals(that.code) && name.equals(that.name);
        }

        @NonNull
        private JSONObject toJson() throws JSONException {
            return new JSONObject().put("code", code).put("name", name);
        }
    }
}
