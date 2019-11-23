package com.gianlu.pretendyourexyzzy.starred;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.pretendyourexyzzy.PK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class StarredDecksManager {
    private static StarredDecksManager instance;
    private final JsonStoring storing;
    private final List<StarredDeck> list;

    private StarredDecksManager() {
        storing = JsonStoring.intoPrefs();
        list = new ArrayList<>();
        loadDecks();
    }

    @NonNull
    public static StarredDecksManager get() {
        if (instance == null) instance = new StarredDecksManager();
        return instance;
    }

    public boolean hasDeck(@NonNull String code) {
        for (StarredDeck deck : list)
            if (Objects.equals(deck.code, code))
                return true;

        return false;
    }

    public void addDeck(@NonNull StarredDeck deck) {
        if (!list.contains(deck)) list.add(deck);
        saveDecks();
    }

    private void saveDecks() {
        try {
            JSONArray array = new JSONArray();
            for (StarredDeck deck : list) array.put(deck.toJson());
            storing.putJsonArray(PK.STARRED_DECKS, array);
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    public void removeDeck(@NonNull String code) {
        Iterator<StarredDeck> iterator = list.iterator();
        while (iterator.hasNext())
            if (Objects.equals(iterator.next().code, code))
                iterator.remove();

        saveDecks();
    }

    private void loadDecks() {
        try {
            list.clear();
            list.addAll(StarredDeck.asList(storing.getJsonArray(PK.STARRED_DECKS)));
            Collections.sort(list, new AddedAtComparator());
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    public boolean hasAnyDeck() {
        return !list.isEmpty();
    }

    @NonNull
    public List<StarredDeck> getDecks() {
        return list;
    }

    private static class AddedAtComparator implements Comparator<StarredDeck> {

        @Override
        public int compare(StarredDeck o1, StarredDeck o2) {
            if (o1.addedAt == o2.addedAt) return 0;
            else return o1.addedAt > o2.addedAt ? -1 : 1;
        }
    }

    public static class StarredDeck {
        public final String code;
        public final String name;
        private final long addedAt;

        public StarredDeck(@NonNull String code, @NonNull String name) {
            this.code = code;
            this.name = name;
            this.addedAt = System.currentTimeMillis();
        }

        private StarredDeck(JSONObject obj) throws JSONException {
            code = obj.getString("code");
            name = obj.getString("name");
            addedAt = obj.optLong("addedAt", System.currentTimeMillis());
        }

        @NonNull
        private static List<StarredDeck> asList(@Nullable JSONArray array) throws JSONException {
            if (array == null) return new ArrayList<>();

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
