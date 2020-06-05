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
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CustomDecksPatchOp;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;

public final class CustomDecksDatabase extends SQLiteOpenHelper {
    private static final int CARD_TYPE_BLACK = 0;
    private static final int CARD_TYPE_WHITE = 1;
    private static final String TAG = CustomDecksDatabase.class.getSimpleName();
    private static CustomDecksDatabase instance;

    private CustomDecksDatabase(@Nullable Context context) {
        super(context, "custom_decks.db", null, 1);
    }

    @NonNull
    public static CustomDecksDatabase get(@NonNull Context context) {
        if (instance == null) instance = new CustomDecksDatabase(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS decks (id INTEGER PRIMARY KEY UNIQUE, name TEXT NOT NULL UNIQUE, watermark TEXT NOT NULL, description TEXT NOT NULL, revision INTEGER NOT NULL, remoteId INTEGER UNIQUE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY UNIQUE, deck_id INTEGER NOT NULL, type INTEGER NOT NULL, text TEXT NOT NULL, remoteId INTEGER UNIQUE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void sendPatch(long revision, long remoteId, @NonNull CustomDecksPatchOp op, @Nullable CustomDeck deck, @Nullable CustomCard card, @Nullable Long cardId) {
        try {
            OverloadedSyncApi.get().patchCustomDeck(revision, remoteId, op, deck == null ? null : deck.toSyncJson(), card == null ? null : card.toSyncJson(), cardId, null, new GeneralCallback<OverloadedSyncApi.CustomDecksUpdateResponse>() {
                @Override
                public void onResult(@NonNull OverloadedSyncApi.CustomDecksUpdateResponse result) {
                    if (op == CustomDecksPatchOp.ADD_CARD && result.cardId != null && card != null)
                        setCardRemoteId(card.id, result.cardId);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed performing patch on server: " + op, ex);
                }
            });
        } catch (JSONException ex) {
            Log.e(TAG, String.format("Failed creating %s patch.", op), ex);
        }
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

    @NonNull
    public List<CustomDeck> getDecks() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM decks", null)) {
            List<CustomDeck> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                list.add(new CustomDeck(cursor));
            }
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
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("watermark", watermark);
            values.put("description", desc);
            values.put("revision", System.currentTimeMillis());
            int id = (int) db.insert("decks", null, values);
            db.setTransactionSuccessful();
            if (id == -1) return null;
            else return new CustomDeck(id, name, watermark, desc, System.currentTimeMillis());
        } finally {
            db.endTransaction();
        }
    }

    public void updateDeckInfo(int id, @NonNull String name, @NonNull String watermark, @NonNull String desc) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        long revision;
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("watermark", watermark);
            values.put("description", desc);
            values.put("revision", revision = System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        CustomDeck deck = getDeck(id);
        if (deck != null && deck.remoteId != null)
            sendPatch(revision, deck.remoteId, CustomDecksPatchOp.EDIT_DECK, deck, null, null);
    }

    private void updateDeckRevision(int id, long revision) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("revision", revision);
            db.update("decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

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

    @NonNull
    public List<CustomCard> getBlackCards(int deckId) {
        return getCards(deckId, CARD_TYPE_BLACK);
    }

    @NonNull
    public List<CustomCard> getWhiteCards(int deckId) {
        return getCards(deckId, CARD_TYPE_WHITE);
    }

    @Nullable
    public CustomCard putCard(int deckId, boolean black, @NonNull String[] text) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) return null;

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

            values = new ContentValues();
            values.put("revision", System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(deckId)});

            db.setTransactionSuccessful();
            card = id == -1 ? null : new CustomCard(text, deck.watermark, type, (int) id);
        } finally {
            db.endTransaction();
        }

        if (card != null) {
            long revision = System.currentTimeMillis();
            updateDeckRevision(deckId, revision);

            if (deck.remoteId != null)
                sendPatch(revision, deck.remoteId, CustomDecksPatchOp.ADD_CARD, null, card, null);
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

        long revision = System.currentTimeMillis();
        updateDeckRevision(deckId, revision);

        if (deck.remoteId != null && remoteId != null)
            sendPatch(revision, deck.remoteId, CustomDecksPatchOp.REM_CARD, null, null, remoteId);
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

        long revision = System.currentTimeMillis();
        updateDeckRevision(deckId, revision);

        if (deck.remoteId != null) {
            if (card.remoteId != null)
                sendPatch(revision, deck.remoteId, CustomDecksPatchOp.EDIT_CARD, null, card, card.remoteId);
            else
                sendPatch(revision, deck.remoteId, CustomDecksPatchOp.ADD_CARD, null, card, null);
        }

        return card;
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
            sendPatch(-1, deck.remoteId, CustomDecksPatchOp.REM_DECK, null, null, null);
    }

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
                    deckId = (int) db.insert("decks", null, values);
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

    public static final class CustomDeck {
        public final int id;
        public final String name;
        public final String watermark;
        public final String description;
        public final Long remoteId;
        public final long revision;

        private CustomDeck(@NonNull Cursor cursor) {
            id = cursor.getInt(cursor.getColumnIndex("id"));
            name = cursor.getString(cursor.getColumnIndex("name"));
            watermark = cursor.getString(cursor.getColumnIndex("watermark"));
            description = cursor.getString(cursor.getColumnIndex("description"));
            revision = cursor.getLong(cursor.getColumnIndex("revision"));

            int remoteIdIndex = cursor.getColumnIndex("remoteId");
            remoteId = cursor.isNull(remoteIdIndex) ? null : cursor.getLong(remoteIdIndex);
        }

        private CustomDeck(int id, @NonNull String name, @NonNull String watermark, @NonNull String description, long revision) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.watermark = watermark;
            this.revision = revision;
            this.remoteId = null;
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
            return !black() ? -1 : text.split("____").length - 1;
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
}
