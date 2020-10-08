package com.gianlu.pretendyourexyzzy.api.crcast;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CrCastDeck extends BasicCustomDeck {
    public final String desc;
    public final CrCastApi.State state;
    public final boolean privateDeck;
    public final String lang;
    public final long created;
    private final int whitesCount;
    private final int blacksCount;
    private Cards cards;

    private CrCastDeck(@NonNull JSONObject obj, @NonNull String watermark, long lastUsed) throws JSONException {
        super(obj.getString("name"), watermark, null, lastUsed, -1);
        desc = obj.getString("description");
        lang = obj.getString("language");
        state = CrCastApi.State.parse(obj.getInt("state"));
        privateDeck = obj.getBoolean("private");
        created = CrCastApi.parseApiDate(obj.getString("createdate"));
        blacksCount = obj.getInt("blackCount");
        whitesCount = obj.getInt("whiteCount");

        cards = new Cards(watermark, obj);
    }

    private CrCastDeck(@NonNull String name, @NonNull String watermark, @NonNull String desc, @NonNull CrCastApi.State state,
                       boolean privateDeck, @NonNull String lang, long created, int whitesCount, int blacksCount, long lastUsed) {
        super(name, watermark, null, lastUsed, -1);
        this.desc = desc;
        this.state = state;
        this.privateDeck = privateDeck;
        this.lang = lang;
        this.created = created;
        this.whitesCount = whitesCount;
        this.blacksCount = blacksCount;
        this.cards = null;
    }

    @Nullable
    @Contract(pure = true)
    public static CrCastDeck find(@NotNull List<CrCastDeck> list, @NonNull String deckCode) {
        for (CrCastDeck deck : list) {
            if (deck.watermark.equals(deckCode))
                return deck;
        }

        return null;
    }

    @NonNull
    public static CrCastDeck fromCached(@NonNull Cursor cursor) {
        return new CrCastDeck(cursor.getString(cursor.getColumnIndex("name")),
                cursor.getString(cursor.getColumnIndex("watermark")),
                cursor.getString(cursor.getColumnIndex("description")),
                CrCastApi.State.parse(cursor.getInt(cursor.getColumnIndex("state"))),
                cursor.getType(cursor.getColumnIndex("private")) == 1,
                cursor.getString(cursor.getColumnIndex("lang")),
                cursor.getLong(cursor.getColumnIndex("created")),
                cursor.getInt(cursor.getColumnIndex("whites_count")),
                cursor.getInt(cursor.getColumnIndex("blacks_count")),
                cursor.getLong(cursor.getColumnIndex("lastUsed")));
    }

    @NonNull
    public static CrCastDeck parse(@NonNull JSONObject obj, @NonNull CustomDecksDatabase db) throws JSONException, ParseException {
        String deckCode = obj.getString("deckcode");
        Long lastUsed = db.getCrCastDeckLastUsed(deckCode);
        if (lastUsed == null) {
            lastUsed = System.currentTimeMillis();
            db.updateCrCastDeckLastUsed(deckCode, lastUsed);
        }

        return new CrCastDeck(obj, deckCode, lastUsed);
    }

    public boolean hasChanged(@NotNull CrCastDeck that) {
        return privateDeck != that.privateDeck || created != that.created ||
                whitesCount != that.whitesCount || blacksCount != that.blacksCount ||
                !desc.equals(that.desc) || state != that.state || !lang.equals(that.lang);
    }

    @Override
    public int whiteCardsCount() {
        return whitesCount;
    }

    @Override
    public int blackCardsCount() {
        return blacksCount;
    }

    @Nullable
    public Cards cards() {
        return cards;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrCastDeck that = (CrCastDeck) o;
        return watermark.equals(that.watermark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(watermark);
    }

    public boolean isAccepted() {
        return state == CrCastApi.State.ACCEPTED;
    }

    @UiThread
    public void getCards(@NonNull CustomDecksDatabase db, @NonNull CrCastApi.DeckCallback callback) {
        if (cards != null) {
            callback.onDeck(this);
            return;
        }

        CrCastApi.get().getDeck(watermark, db, null, new CrCastApi.DeckCallback() {
            @Override
            public void onDeck(@NonNull CrCastDeck deck) {
                CrCastDeck.this.cards = deck.cards;
                callback.onDeck(CrCastDeck.this);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                callback.onException(ex);
            }
        });
    }

    @NonNull
    public JSONObject craftPyxJson() throws JSONException {
        if (cards == null) throw new IllegalStateException();

        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("description", desc);
        obj.put("watermark", watermark);

        JSONArray callsArray = new JSONArray();
        for (CrCastCard card : cards.blacks) callsArray.put(card.craftJson());
        obj.put("calls", callsArray);

        JSONArray responsesArray = new JSONArray();
        for (CrCastCard card : cards.whites) responsesArray.put(card.craftJson());
        obj.put("responses", responsesArray);

        return obj;
    }

    public static class Cards {
        public final List<CrCastCard> blacks;
        public final List<CrCastCard> whites;

        Cards(@NonNull String watermark, @NotNull JSONObject obj) throws JSONException {
            JSONArray blacksArray = obj.getJSONArray("blacks");
            blacks = new ArrayList<>(blacksArray.length());
            for (int i = 0; i < blacksArray.length(); i++)
                blacks.add(new CrCastCard(watermark, true, blacksArray.getJSONObject(i)));

            JSONArray whitesArray = obj.getJSONArray("whites");
            whites = new ArrayList<>(whitesArray.length());
            for (int i = 0; i < whitesArray.length(); i++)
                whites.add(new CrCastCard(watermark, false, whitesArray.getJSONObject(i)));
        }
    }
}
