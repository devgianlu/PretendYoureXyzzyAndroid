package com.gianlu.pretendyourexyzzy.api.crcast;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CrCastDeck extends BasicCustomDeck {
    private static final String TAG = CrCastDeck.class.getSimpleName();
    public final String desc;
    public final CrCastApi.State state;
    public final boolean privateDeck;
    public final String lang;
    public final Long created;
    public final boolean favorite;
    private final int whitesCount;
    private final int blacksCount;
    private Cards cards;

    private CrCastDeck(@NonNull JSONObject obj, @NonNull String watermark, boolean favorite, long lastUsed) throws JSONException {
        super(obj.getString("name"), watermark, null, lastUsed, -1);
        this.desc = obj.getString("description");
        this.lang = obj.getString("language").toUpperCase();
        this.privateDeck = obj.optBoolean("private", false);
        this.favorite = favorite;

        if (obj.has("createdate"))
            this.created = CrCastApi.parseApiDate(obj.getString("createdate"));
        else
            this.created = null;

        int stateInt = obj.optInt("state", -1);
        if (stateInt == -1) this.state = null;
        else this.state = CrCastApi.State.parse(obj.getInt("state"));

        int blacks = obj.optInt("blackCount", -1);
        int whites = obj.optInt("whiteCount", -1);

        try {
            this.cards = new Cards(watermark, obj);
            blacks = cards.blacks.size();
            whites = cards.whites.size();
        } catch (JSONException ex) {
            Log.w(TAG, "Failed parsing deck cards!", ex);
            this.cards = null;
        }

        this.blacksCount = blacks;
        this.whitesCount = whites;
    }

    private CrCastDeck(@NonNull String name, @NonNull String watermark, @NonNull String desc, @Nullable CrCastApi.State state,
                       boolean privateDeck, @NonNull String lang, @Nullable Long created, int whitesCount, int blacksCount, boolean favorite, long lastUsed) {
        super(name, watermark, null, lastUsed, -1);
        this.desc = desc;
        this.state = state;
        this.privateDeck = privateDeck;
        this.lang = lang;
        this.created = created;
        this.whitesCount = whitesCount;
        this.blacksCount = blacksCount;
        this.favorite = favorite;
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
        int stateIndex = cursor.getColumnIndex("state");
        CrCastApi.State state;
        if (cursor.isNull(stateIndex)) state = null;
        else state = CrCastApi.State.parse(cursor.getInt(stateIndex));

        int createdIndex = cursor.getColumnIndex("created");
        Long created;
        if (cursor.isNull(createdIndex)) created = null;
        else created = cursor.getLong(createdIndex);

        return new CrCastDeck(cursor.getString(cursor.getColumnIndex("name")),
                cursor.getString(cursor.getColumnIndex("watermark")),
                cursor.getString(cursor.getColumnIndex("description")),
                state,
                cursor.getInt(cursor.getColumnIndex("private")) == 1,
                cursor.getString(cursor.getColumnIndex("lang")),
                created,
                cursor.getInt(cursor.getColumnIndex("whites_count")),
                cursor.getInt(cursor.getColumnIndex("blacks_count")),
                cursor.getInt(cursor.getColumnIndex("favorite")) == 1,
                cursor.getLong(cursor.getColumnIndex("lastUsed")));
    }

    @NonNull
    public static CrCastDeck parse(@NonNull JSONObject obj, @NonNull CustomDecksDatabase db, boolean favorite) throws JSONException {
        String deckCode = obj.getString("deckcode");
        Long lastUsed = db.getCrCastDeckLastUsed(deckCode);
        if (lastUsed == null) {
            lastUsed = System.currentTimeMillis();
            db.updateCrCastDeckLastUsed(deckCode, lastUsed);
        }

        return new CrCastDeck(obj, deckCode, favorite, lastUsed);
    }

    public boolean hasChanged(@NotNull CrCastDeck that) {
        return privateDeck != that.privateDeck || !Objects.equals(created, that.created) ||
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
    public Task<CrCastDeck> getCards(@NonNull CustomDecksDatabase db) {
        if (cards != null) return Tasks.forResult(this);

        return CrCastApi.get().getDeck(watermark, favorite, db)
                .continueWith(task -> {
                    this.cards = task.getResult().cards;
                    return this;
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
