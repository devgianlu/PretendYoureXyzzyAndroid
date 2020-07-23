package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CustomDecksPatchOp;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.StarredCustomDecksPatchOp;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

public final class CustomDecksDatabase extends SQLiteOpenHelper {
    private static final int CARD_TYPE_BLACK = 0;
    private static final int CARD_TYPE_WHITE = 1;
    private static final String TAG = CustomDecksDatabase.class.getSimpleName();
    private static CustomDecksDatabase instance;

    private CustomDecksDatabase(@Nullable Context context) {
        super(context, "custom_decks.db", null, 6);
    }

    @NonNull
    public static CustomDecksDatabase get(@NonNull Context context) {
        if (instance == null) instance = new CustomDecksDatabase(context);
        return instance;
    }

    public static long getStaredCustomDecksRevision() {
        return Prefs.getLong(PK.STARRED_CUSTOM_DECKS_REVISION, 0);
    }

    public static void setStaredCustomDecksRevision(long revision) {
        Prefs.putLong(PK.STARRED_CUSTOM_DECKS_REVISION, revision);
    }

    @NonNull
    public static List<FloatingCustomDeck> transform(@NotNull String owner, @NonNull List<UserProfile.CustomDeck> original) {
        List<FloatingCustomDeck> list = new ArrayList<>(original.size());
        for (UserProfile.CustomDeck deck : original)
            list.add(new FloatingCustomDeck(deck.name, deck.watermark, owner, deck.count));
        return list;
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS decks (id INTEGER PRIMARY KEY UNIQUE, name TEXT NOT NULL UNIQUE, watermark TEXT NOT NULL, description TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 0, remoteId INTEGER UNIQUE, lastUsed INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY UNIQUE, deck_id INTEGER NOT NULL, type INTEGER NOT NULL, text TEXT NOT NULL, remoteId INTEGER UNIQUE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS starred_decks (id INTEGER PRIMARY KEY UNIQUE, shareCode TEXT NOT NULL UNIQUE, name TEXT NOT NULL UNIQUE, watermark TEXT NOT NULL, owner TEXT NOT NULL, cards_count INTEGER NOT NULL, remoteId INTEGER UNIQUE, lastUsed INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading from " + oldVersion + " to " + newVersion);
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE decks ADD revision INTEGER NOT NULL DEFAULT 0");
                db.execSQL("ALTER TABLE decks ADD remoteId INTEGER");
                db.execSQL("CREATE UNIQUE INDEX remoteId_decks_unique ON decks(remoteId)");

                db.execSQL("ALTER TABLE cards ADD remoteId INTEGER");
                db.execSQL("CREATE UNIQUE INDEX remoteId_cards_unique ON cards(remoteId)");

                db.execSQL("CREATE TABLE IF NOT EXISTS starred_decks (id INTEGER PRIMARY KEY UNIQUE, shareCode TEXT NOT NULL UNIQUE, name TEXT NOT NULL UNIQUE, watermark TEXT NOT NULL, owner TEXT NOT NULL, cards_count INTEGER NOT NULL, remoteId INTEGER UNIQUE)");
            case 5:
                db.execSQL("ALTER TABLE decks ADD lastUsed INTEGER NOT NULL DEFAULT 0");
                db.execSQL("ALTER TABLE starred_decks ADD lastUsed INTEGER NOT NULL DEFAULT 0");
        }

        Log.i(TAG, "Migrated database from " + oldVersion + " to " + newVersion);
    }

    private void sendCustomDeckPatch(long revision, @Nullable Long remoteId, @NonNull CustomDecksPatchOp op, @Nullable CustomDeck deck, @Nullable CustomCard card, @Nullable Long cardId, @Nullable List<CustomCard> cards) {
        if (!OverloadedApi.get().isFullyRegistered())
            return;

        JSONArray cardsJson = null;
        if (cards != null) {
            cardsJson = new JSONArray();
            try {
                for (CustomCard cc : cards)
                    cardsJson.put(cc.toSyncJson());
            } catch (JSONException ex) {
                Log.e(TAG, "Failed creating custom decks patch cards payload.", ex);
                return;
            }
        }

        try {
            Log.d(TAG, "Sending custom deck patch: " + op);
            OverloadedSyncApi.get().patchCustomDeck(revision, remoteId, op, deck == null ? null : deck.toSyncJson(), card == null ? null : card.toSyncJson(), cardId, cardsJson, null, new GeneralCallback<OverloadedSyncApi.CustomDecksUpdateResponse>() {
                @Override
                public void onResult(@NonNull OverloadedSyncApi.CustomDecksUpdateResponse result) {
                    if (op == CustomDecksPatchOp.ADD_CARD && result.cardId != null && card != null) {
                        setCardRemoteId(card.id, result.cardId);
                    } else if (op == CustomDecksPatchOp.ADD_DECK && result.deckId != null && deck != null) {
                        setDeckRemoteId(deck.id, result.deckId);
                    } else if (op == CustomDecksPatchOp.ADD_CARDS && result.cardsIds != null && cards != null) {
                        if (result.cardsIds.length != cards.size()) {
                            Log.e(TAG, String.format("IDs number doesn't match, local: %d, remote: %d", cards.size(), result.cardsIds.length));
                            return;
                        }

                        for (int i = 0; i < result.cardsIds.length; i++)
                            setCardRemoteId(cards.get(i).id, result.cardsIds[i]);
                    }

                    Log.d(TAG, "Completed custom deck patch: " + op);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed performing patch for custom decks on server: " + op, ex);
                }
            });
        } catch (JSONException ex) {
            Log.e(TAG, String.format("Failed creating %s patch for custom decks.", op), ex);
        }
    }

    private void sendStarredDeckPatch(long revision, @Nullable Long remoteId, @NonNull StarredCustomDecksPatchOp op, @Nullable String shareCode, @Nullable Integer deckId) {
        if (!OverloadedApi.get().isFullyRegistered())
            return;

        OverloadedSyncApi.get().patchStarredCustomDecks(revision, op, remoteId, shareCode, null, new GeneralCallback<OverloadedSyncApi.StarredCustomDecksUpdateResponse>() {
            @Override
            public void onResult(@NonNull OverloadedSyncApi.StarredCustomDecksUpdateResponse result) {
                if (op == StarredCustomDecksPatchOp.ADD && result.remoteId != null && deckId != null)
                    setStarredDeckRemoteId(deckId, result.remoteId);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed performing patch on server: " + op, ex);
            }
        });
    }

    public boolean isNameUnique(@NonNull String name) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM decks WHERE name=?", new String[]{name})) {
            if (cursor == null || !cursor.moveToNext()) return false;
            else return cursor.getInt(0) == 0;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteDeckAndCards(int deckId, boolean remote) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) return;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("decks", "id=?", new String[]{String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.beginTransaction();
        try {
            db.delete("cards", "deck_id=?", new String[]{String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (remote && deck.remoteId != null)
            sendCustomDeckPatch(-1, deck.remoteId, CustomDecksPatchOp.REM_DECK, null, null, null, null);
    }


    //////////////////////////////////
    ///////// Starred decks //////////
    //////////////////////////////////

    @NonNull
    public List<StarredDeck> getStarredDecks(boolean leftover) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery(leftover ? "SELECT * FROM starred_decks WHERE remoteId IS NULL" : "SELECT * FROM starred_decks", null)) {
            List<StarredDeck> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) list.add(new StarredDeck(cursor));
            return list;
        } finally {
            db.endTransaction();
        }
    }

    public boolean isStarred(@NonNull String shareCode) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM starred_decks WHERE shareCode=?", new String[]{shareCode})) {
            if (!cursor.moveToNext()) return false;
            else return cursor.getInt(0) > 0;
        } finally {
            db.endTransaction();
        }
    }

    public void updateStarredDeckLastUsed(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("lastUsed", System.currentTimeMillis());
            db.update("starred_decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addStarredDeck(@NonNull String shareCode, @NonNull String name, @NonNull String watermark, @NonNull String owner, int cardsCount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        int id;
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("shareCode", shareCode);
            values.put("watermark", watermark);
            values.put("owner", owner);
            values.put("cards_count", cardsCount);
            values.put("lastUsed", System.currentTimeMillis());
            id = (int) db.insert("starred_decks", null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        long revision = OverloadedApi.now();
        setStaredCustomDecksRevision(revision);

        if (id != -1)
            sendStarredDeckPatch(revision, null, StarredCustomDecksPatchOp.ADD, shareCode, id);
    }

    public void removeStarredDeck(@NonNull String owner, @NonNull String shareCode) {
        Long remoteId = getStarredDeckRemoteId(shareCode);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("starred_decks", "owner=? AND shareCode=?", new String[]{owner, shareCode});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        long revision = OverloadedApi.now();
        setStaredCustomDecksRevision(revision);

        if (remoteId != null)
            sendStarredDeckPatch(revision, remoteId, StarredCustomDecksPatchOp.REM, null, null);
    }

    @NonNull
    public List<FloatingCustomDeck> getAllDecks() {
        List<FloatingCustomDeck> decks = new LinkedList<>();
        decks.addAll(getDecks());
        decks.addAll(getStarredDecks(false));
        Collections.sort(decks, (o1, o2) -> Long.compare(o2.lastUsed, o1.lastUsed));
        return decks;
    }

    public void loadStarredDecksUpdate(@NonNull JSONArray update, boolean delete, @Nullable Long revision) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (delete) db.execSQL("DELETE FROM starred_decks WHERE remoteId IS NOT NULL");

            for (int i = 0; i < update.length(); i++) {
                JSONObject obj = update.getJSONObject(i);
                JSONObject deckObj = obj.getJSONObject("deck");

                ContentValues values = new ContentValues();
                values.put("shareCode", deckObj.getString("shareCode"));
                values.put("name", deckObj.getString("name"));
                values.put("watermark", deckObj.getString("watermark"));
                values.put("owner", obj.getString("owner"));
                values.put("cards_count", deckObj.getInt("count"));
                values.put("remoteId", obj.getLong("remoteId"));
                values.put("lastUsed", System.currentTimeMillis());
                db.insert("starred_decks", null, values);
            }

            db.setTransactionSuccessful();
            if (revision != null) setStaredCustomDecksRevision(revision);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed adding starred custom decks.", ex);
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public UpdatePair getStarredDecksUpdate() {
        JSONArray array = new JSONArray();
        List<StarredDeck> list = getStarredDecks(false);
        long[] localIds = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            StarredDeck deck = list.get(i);
            localIds[i] = deck.id;

            try {
                JSONObject obj = new JSONObject();
                obj.put("remoteId", deck.remoteId);
                obj.put("shareCode", deck.shareCode);
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }

        return new UpdatePair(array, localIds);
    }


    //////////////////////////////////
    ///////////// Decks //////////////
    //////////////////////////////////

    @NonNull
    public List<CustomDeck> getDecks() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM decks", null)) {
            List<CustomDeck> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) list.add(new CustomDeck(cursor));
            return list;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public CustomDeck getDeck(int id) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM decks WHERE id=?", new String[]{String.valueOf(id)})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            return new CustomDeck(cursor);
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public CustomDeck getDeckByRemoteId(long remoteId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM decks WHERE remoteId=?", new String[]{String.valueOf(remoteId)})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            return new CustomDeck(cursor);
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public CustomDeck putDeckInfo(@NonNull String name, @NonNull String watermark, @NonNull String desc) {
        long revision = OverloadedApi.now();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long lastUsed = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("watermark", watermark);
            values.put("description", desc);
            values.put("revision", revision);
            values.put("lastUsed", lastUsed);
            int id = (int) db.insert("decks", null, values);
            db.setTransactionSuccessful();
            if (id == -1) return null;

            CustomDeck deck = new CustomDeck(id, name, watermark, desc, lastUsed, revision);
            sendCustomDeckPatch(deck.revision, null, CustomDecksPatchOp.ADD_DECK, deck, null, null, null);
            return deck;
        } finally {
            db.endTransaction();
        }
    }

    public void updateDeckLastUsed(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("lastUsed", System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateDeckInfo(int id, @NonNull String name, @NonNull String watermark, @NonNull String desc) {
        long revision = OverloadedApi.now();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("watermark", watermark);
            values.put("description", desc);
            values.put("revision", revision);
            values.put("lastUsed", System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        CustomDeck deck = getDeck(id);
        if (deck != null && deck.remoteId != null)
            sendCustomDeckPatch(revision, deck.remoteId, CustomDecksPatchOp.EDIT_DECK, deck, null, null, null);
    }

    private void updateDeckRevision(int id, long revision) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("revision", revision);
            values.put("lastUsed", System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void loadDeckUpdate(@NonNull JSONObject update, boolean isNew) {
        Integer deckId;
        try {
            JSONObject deck = update.getJSONObject("deck");
            long remoteId = deck.getLong("remoteId");
            deckId = getDeckIdByRemoteId(remoteId);

            if (isNew || deckId == null) {
                if (deckId != null) deleteDeckAndCards(deckId, false);

                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    ContentValues values = new ContentValues();
                    values.put("name", deck.getString("name"));
                    values.put("description", deck.getString("desc"));
                    values.put("watermark", deck.getString("watermark"));
                    values.put("revision", deck.getLong("rev"));
                    values.put("remoteId", remoteId);
                    values.put("lastUsed", System.currentTimeMillis());
                    deckId = (int) db.insertWithOnConflict("decks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                    if (deckId == -1) {
                        Log.e(TAG, "Failed inserting custom deck.");
                        return;
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            } else {
                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    ContentValues values = new ContentValues();
                    values.put("name", deck.getString("name"));
                    values.put("description", deck.getString("desc"));
                    values.put("watermark", deck.getString("watermark"));
                    values.put("revision", deck.getLong("rev"));
                    values.put("lastUsed", System.currentTimeMillis());
                    values.put("remoteId", remoteId);
                    db.update("decks", values, "id=?", new String[]{String.valueOf(deckId)});
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed parsing deck update.", ex);
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            JSONArray cards = update.getJSONArray("cards");
            for (int i = 0; i < cards.length(); i++) {
                JSONObject card = cards.getJSONObject(i);

                ContentValues values = new ContentValues();
                values.put("deck_id", deckId);
                values.put("text", card.getString("text"));
                values.put("type", card.getLong("type"));
                db.insert("cards", null, values);
            }

            db.setTransactionSuccessful();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed parsing cards update.", ex);
        } finally {
            db.endTransaction();
        }
    }


    //////////////////////////////////
    ///////////// Cards //////////////
    //////////////////////////////////

    @NonNull
    public List<CustomCard> getCards(int deckId) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) throw new IllegalStateException();

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM cards WHERE deck_id=?", new String[]{String.valueOf(deckId)})) {
            if (cursor == null) return new ArrayList<>(0);

            List<CustomCard> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) list.add(new CustomCard(deck, cursor));
            return list;
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<CustomCard> getCards(int deckId, int type) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) throw new IllegalStateException();

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM cards WHERE type=? AND deck_id=?", new String[]{String.valueOf(type), String.valueOf(deckId)})) {
            if (cursor == null) return new ArrayList<>(0);

            List<CustomCard> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) list.add(new CustomCard(deck, cursor));
            return list;
        } finally {
            db.endTransaction();
        }
    }

    private int countCards(int deckId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM cards WHERE deck_id=?", new String[]{String.valueOf(deckId)})) {
            if (cursor == null || !cursor.moveToNext()) return 0;
            else return cursor.getInt(0);
        } finally {
            db.endTransaction();
        }
    }

    private int countCards(int deckId, int type) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM cards WHERE type=? AND deck_id=?", new String[]{String.valueOf(type), String.valueOf(deckId)})) {
            if (cursor == null || !cursor.moveToNext()) return 0;
            else return cursor.getInt(0);
        } finally {
            db.endTransaction();
        }
    }

    private int countBlackCards(int deckId) {
        return countCards(deckId, CARD_TYPE_BLACK);
    }

    private int countWhiteCards(int deckId) {
        return countCards(deckId, CARD_TYPE_WHITE);
    }

    @NonNull
    public List<CustomCard> getBlackCards(int deckId) {
        return getCards(deckId, CARD_TYPE_BLACK);
    }

    @NonNull
    public List<CustomCard> getWhiteCards(int deckId) {
        return getCards(deckId, CARD_TYPE_WHITE);
    }

    @NonNull
    public List<CustomCard> putCards(int deckId, boolean[] blacks, @NonNull String[][] texts) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) return Collections.emptyList();

        long revision = OverloadedApi.now();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        List<CustomCard> cards = new ArrayList<>(blacks.length);
        try {
            for (int i = 0; i < blacks.length; i++) {
                String[] text = texts[i];
                int type = blacks[i] ? CARD_TYPE_BLACK : CARD_TYPE_WHITE;

                ContentValues values = new ContentValues();
                values.put("deck_id", deckId);
                values.put("type", type);
                values.put("text", CommonUtils.join(text, "____"));
                long id = db.insert("cards", null, values);
                CustomCard card = id == -1 ? null : new CustomCard(text, deck.watermark, type, (int) id);
                if (card != null) cards.add(card);
                else Log.e(TAG, "Failed adding card at " + i);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        updateDeckRevision(deckId, revision);
        if (deck.remoteId != null) // FIXME: This will always be null
            sendCustomDeckPatch(revision, deck.remoteId, CustomDecksPatchOp.ADD_CARDS, null, null, null, cards);

        return cards;
    }

    @Nullable
    public CustomCard putCard(int deckId, boolean black, @NonNull String[] text) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) return null;

        long revision = OverloadedApi.now();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        CustomCard card;
        try {
            int type = black ? CARD_TYPE_BLACK : CARD_TYPE_WHITE;

            ContentValues values = new ContentValues();
            values.put("deck_id", deckId);
            values.put("type", type);
            values.put("text", CommonUtils.join(text, "____"));
            long id = db.insert("cards", null, values);

            db.setTransactionSuccessful();
            card = id == -1 ? null : new CustomCard(text, deck.watermark, type, (int) id);
        } finally {
            db.endTransaction();
        }

        if (card != null) {
            updateDeckRevision(deckId, revision);

            if (deck.remoteId != null)
                sendCustomDeckPatch(revision, deck.remoteId, CustomDecksPatchOp.ADD_CARD, null, card, null, null);
        }

        return card;
    }

    public void removeCard(int deckId, int cardId) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null)
            return;

        Long remoteId = getCardRemoteId(cardId);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("cards", "id=? AND deck_id=?", new String[]{String.valueOf(cardId), String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        long revision = OverloadedApi.now();
        updateDeckRevision(deckId, revision);

        if (deck.remoteId != null && remoteId != null)
            sendCustomDeckPatch(revision, deck.remoteId, CustomDecksPatchOp.REM_CARD, null, null, remoteId, null);
    }

    @Nullable
    public CustomCard updateCard(int deckId, @NonNull CustomCard old, @NonNull String[] text) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null)
            return null;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        CustomCard card;
        try {
            ContentValues values = new ContentValues();
            values.put("text", CommonUtils.join(text, "____"));
            db.update("cards", values, "id=?", new String[]{String.valueOf(old.id)});
            db.setTransactionSuccessful();
            card = new CustomCard(old, text);
        } finally {
            db.endTransaction();
        }

        long revision = OverloadedApi.now();
        updateDeckRevision(deckId, revision);

        if (deck.remoteId != null) {
            if (card.remoteId != null)
                sendCustomDeckPatch(revision, deck.remoteId, CustomDecksPatchOp.EDIT_CARD, null, card, card.remoteId, null);
            else
                sendCustomDeckPatch(revision, deck.remoteId, CustomDecksPatchOp.ADD_CARD, null, card, null, null);
        }

        return card;
    }


    //////////////////////////////////
    /////////// Remote IDs ///////////
    //////////////////////////////////

    public void resetRemoteIds(int deckId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.putNull("remoteId");
            db.update("decks", values, "id=?", new String[]{String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.putNull("remoteId");
            db.update("cards", values, "deck_id=?", new String[]{String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void setDeckRemoteId(long deckId, long remoteId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("remoteId", remoteId);
            db.update("decks", values, "id=?", new String[]{String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void setCardRemoteId(int cardId, long remoteId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("remoteId", remoteId);
            db.update("cards", values, "id=?", new String[]{String.valueOf(cardId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    private Long getCardRemoteId(int cardId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT remoteId FROM cards WHERE id=?", new String[]{String.valueOf(cardId)})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            else return cursor.getLong(0);
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public Integer getDeckIdByRemoteId(long remoteId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT id FROM decks WHERE remoteId=?", new String[]{String.valueOf(remoteId)})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            else return cursor.getInt(0);
        } finally {
            db.endTransaction();
        }
    }

    public void setStarredDeckRemoteId(long id, long remoteId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("remoteId", remoteId);
            db.update("starred_decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    private Long getStarredDeckRemoteId(@NonNull String shareCode) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT remoteId FROM starred_decks WHERE shareCode=?", new String[]{shareCode})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            else return cursor.getLong(0);
        } finally {
            db.endTransaction();
        }
    }

    public static class UpdatePair {
        public final JSONArray update;
        public final long[] localIds;

        private UpdatePair(@NonNull JSONArray array, @NonNull long[] localIds) {
            this.update = array;
            this.localIds = localIds;
        }
    }

    public static class FloatingCustomDeck {
        public final String name;
        public final String watermark;
        public final String owner;
        public final long lastUsed;
        private final int count;

        FloatingCustomDeck(@NonNull String name, @NonNull String watermark, @Nullable String owner, long lastUsed) {
            this(name, watermark, owner, lastUsed, -1);
        }

        FloatingCustomDeck(@NonNull String name, @NonNull String watermark, @Nullable String owner, long lastUsed, int count) {
            this.name = name;
            this.watermark = watermark;
            this.owner = owner;
            this.lastUsed = lastUsed;
            this.count = count;
        }

        public int cardsCount() {
            return count;
        }

        public int whiteCardsCount() {
            return -1;
        }

        public int blackCardsCount() {
            return -1;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + watermark.hashCode();
            return result;
        }
    }

    public static final class CustomCard extends BaseCard {
        public final int id;
        private final String text;
        private final String watermark;
        private final int type;
        private final Long remoteId;

        private CustomCard(String[] text, String watermark, int type, int id) {
            this.text = CommonUtils.join(text, "____");
            this.watermark = watermark;
            this.type = type;
            this.id = id;
            this.remoteId = null;
        }

        private CustomCard(@NonNull CustomCard card, @NonNull String[] text) {
            this.text = CommonUtils.join(text, "____");
            this.watermark = card.watermark;
            this.type = card.type;
            this.id = card.id;
            this.remoteId = card.remoteId;
        }

        private CustomCard(@NonNull CustomDeck deck, @NonNull Cursor cursor) {
            watermark = deck.watermark;

            type = cursor.getInt(cursor.getColumnIndex("type"));
            id = cursor.getInt(cursor.getColumnIndex("id"));
            text = cursor.getString(cursor.getColumnIndex("text"));

            int remoteIdIndex = cursor.getColumnIndex("remoteId");
            remoteId = cursor.isNull(remoteIdIndex) ? null : cursor.getLong(remoteIdIndex);
        }

        @NonNull
        public static JSONArray toSyncJson(@NonNull List<CustomCard> cards) throws JSONException {
            JSONArray array = new JSONArray();
            for (CustomCard card : cards) array.put(card.toSyncJson());
            return array;
        }

        @NonNull
        public static BaseCard createTemp(String[] text, String watermark, boolean black) {
            return new CustomCard(text, watermark, black ? CARD_TYPE_BLACK : CARD_TYPE_WHITE, Integer.MIN_VALUE);
        }

        @NonNull
        JSONObject craftJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("text", CommonUtils.toJSONArray(text.split("____")));
            return obj;
        }

        @NonNull
        JSONObject toSyncJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("text", text());
            obj.put("type", type);
            obj.put("id", remoteId);
            return obj;
        }

        @NonNull
        @Override
        public String text() {
            return text;
        }

        @NonNull
        @Override
        public String watermark() {
            return watermark;
        }

        @Override
        public int numPick() {
            return !black() ? -1 : text.split("____", -1).length - 1;
        }

        @Override
        public int numDraw() {
            return !black() ? -1 : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomCard that = (CustomCard) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean black() {
            return type == CARD_TYPE_BLACK;
        }
    }

    public static class StarredDeck extends FloatingCustomDeck {
        public final String shareCode;
        public final int id;
        public final Long remoteId;

        public StarredDeck(@NonNull Cursor cursor) {
            super(cursor.getString(cursor.getColumnIndex("name")), cursor.getString(cursor.getColumnIndex("watermark")),
                    cursor.getString(cursor.getColumnIndex("owner")), cursor.getInt(cursor.getColumnIndex("cards_count")));
            shareCode = cursor.getString(cursor.getColumnIndex("shareCode"));
            id = cursor.getInt(cursor.getColumnIndex("id"));

            int remoteIdIndex = cursor.getColumnIndex("remoteId");
            remoteId = cursor.isNull(remoteIdIndex) ? null : cursor.getLong(remoteIdIndex);
        }
    }

    public final class CustomDeck extends FloatingCustomDeck {
        public final int id;
        public final String description;
        public final Long remoteId;
        public final long revision;

        private CustomDeck(@NonNull Cursor cursor) {
            super(cursor.getString(cursor.getColumnIndex("name")), cursor.getString(cursor.getColumnIndex("watermark")), null, cursor.getLong(cursor.getColumnIndex("lastUsed")));
            id = cursor.getInt(cursor.getColumnIndex("id"));
            description = cursor.getString(cursor.getColumnIndex("description"));
            revision = cursor.getLong(cursor.getColumnIndex("revision"));

            int remoteIdIndex = cursor.getColumnIndex("remoteId");
            remoteId = cursor.isNull(remoteIdIndex) ? null : cursor.getLong(remoteIdIndex);
        }

        private CustomDeck(int id, @NonNull String name, @NonNull String watermark, @NonNull String description, long lastUsed, long revision) {
            super(name, watermark, null, lastUsed);
            this.id = id;
            this.description = description;
            this.revision = revision;
            this.remoteId = null;
        }

        @Override
        public int cardsCount() {
            return countCards(id);
        }

        @Override
        public int whiteCardsCount() {
            return countWhiteCards(id);
        }

        @Override
        public int blackCardsCount() {
            return countBlackCards(id);
        }

        @Override
        public int hashCode() {
            return id;
        }

        @NonNull
        public JSONObject toSyncJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("desc", description);
            obj.put("watermark", watermark);
            obj.put("id", remoteId);
            return obj;
        }

        @NonNull
        public JSONObject craftPyxJson(@NonNull CustomDecksDatabase db) throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("description", description);
            obj.put("watermark", watermark);

            List<CustomCard> calls = db.getBlackCards(id);
            JSONArray callsArray = new JSONArray();
            for (CustomCard card : calls) callsArray.put(card.craftJson());
            obj.put("calls", callsArray);

            List<CustomCard> responses = db.getWhiteCards(id);
            JSONArray responsesArray = new JSONArray();
            for (CustomCard card : responses) responsesArray.put(card.craftJson());
            obj.put("responses", responsesArray);

            return obj;
        }

        @NotNull
        @Contract(pure = true)
        @Override
        public String toString() {
            return "CustomDeck{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", remoteId=" + remoteId +
                    ", revision=" + revision +
                    '}';
        }
    }
}
