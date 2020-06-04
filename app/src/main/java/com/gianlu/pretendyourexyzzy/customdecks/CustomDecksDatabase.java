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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class CustomDecksDatabase extends SQLiteOpenHelper {
    private static final int CARD_TYPE_BLACK = 0;
    private static final int CARD_TYPE_WHITE = 1;
    private static final String TAG = CustomDecksDatabase.class.getSimpleName();
    private static CustomDecksDatabase instance;

    private CustomDecksDatabase(@Nullable Context context) {
        super(context, "custom_decks.db", null, 2);
    }

    @NonNull
    public static CustomDecksDatabase get(@NonNull Context context) {
        if (instance == null) instance = new CustomDecksDatabase(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS decks (id INTEGER PRIMARY KEY UNIQUE, name TEXT NOT NULL UNIQUE, watermark TEXT NOT NULL, description TEXT NOT NULL, revision INTEGER NOT NULL, remoteId INTEGER UNIQUE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY UNIQUE, deck_id INTEGER NOT NULL, type INTEGER NOT NULL, text TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE decks ADD remoteId INTEGER");
            db.execSQL("ALTER TABLE decks ADD revision INTEGER NOT NULL");
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
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("watermark", watermark);
            values.put("description", desc);
            values.put("revision", System.currentTimeMillis());
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
        try {
            int type = black ? CARD_TYPE_BLACK : CARD_TYPE_WHITE;

            ContentValues values = new ContentValues();
            values.put("deck_id", deckId);
            values.put("type", type);
            values.put("text", CommonUtils.toJSONArray(text).toString());
            long id = db.insert("cards", null, values);

            values = new ContentValues();
            values.put("revision", System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(deckId)});

            db.setTransactionSuccessful();
            if (id == -1) return null;
            else return new CustomCard(text, deck.watermark, type, (int) id);
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public CustomCard updateCard(int deckId, @NonNull CustomCard old, @NonNull String[] text) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("text", CommonUtils.toJSONArray(text).toString());
            db.update("cards", values, "id=?", new String[]{String.valueOf(old.id)});

            values = new ContentValues();
            values.put("revision", System.currentTimeMillis());
            db.update("decks", values, "id=?", new String[]{String.valueOf(deckId)});

            db.setTransactionSuccessful();
            return new CustomCard(old, text);
        } finally {
            db.endTransaction();
        }
    }

    public void deleteDeckAndCards(int deckId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("decks", "id=?", new String[]{String.valueOf(deckId)});
            db.delete("cards", "deck_id=?", new String[]{String.valueOf(deckId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void setRemoteId(long deckId, long remoteId) {
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

    @Nullable
    private Long getDeckIdByRemoteId(long remoteId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT id FROM decks WHERE remoteId=?", new String[]{String.valueOf(remoteId)})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            else return cursor.getLong(0);
        } finally {
            db.endTransaction();
        }
    }

    public void loadDeckUpdate(@NonNull JSONObject update) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            JSONObject deck = update.getJSONObject("deck");
            long remoteId = deck.getLong("remoteId");
            Long oldId = getDeckIdByRemoteId(remoteId);
            if (oldId != null) {
                db.delete("decks", "id=?", new String[]{String.valueOf(oldId)});
                db.delete("cards", "deck_id=?", new String[]{String.valueOf(oldId)});
            }

            ContentValues values = new ContentValues();
            values.put("name", deck.getString("name"));
            values.put("description", deck.getString("desc"));
            values.put("watermark", deck.getString("watermark"));
            values.put("revision", deck.getLong("rev"));
            values.put("remoteId", remoteId);
            long id = db.insert("decks", null, values);
            if (id == -1) {
                Log.e(TAG, "Failed inserting new deck.");
                return;
            }

            JSONArray cards = update.getJSONArray("cards");
            for (int i = 0; i < cards.length(); i++) {
                JSONObject card = cards.getJSONObject(i);

                values = new ContentValues();
                values.put("deck_id", id);
                values.put("text", card.getString("text"));
                values.put("watermark", card.getString("watermark"));
                values.put("type", card.getLong("type"));
                db.insert("cards", null, values);
            }

            db.setTransactionSuccessful();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed parsing update.", ex);
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
    }

    public static final class CustomCard extends BaseCard {
        public final int id;
        private final String[] text;
        private final String watermark;
        private final int type;
        private transient String sentence = null;

        private CustomCard(String[] text, String watermark, int type, int id) {
            this.text = text;
            this.watermark = watermark;
            this.type = type;
            this.id = id;
        }

        private CustomCard(@NonNull CustomCard card, @NonNull String[] text) {
            this.text = text;
            this.watermark = card.watermark;
            this.type = card.type;
            this.id = card.id;
        }

        private CustomCard(@NonNull CustomDeck deck, @NonNull Cursor cursor) {
            watermark = deck.watermark;

            type = cursor.getInt(cursor.getColumnIndex("type"));
            id = cursor.getInt(cursor.getColumnIndex("id"));

            try {
                text = CommonUtils.toStringArray(new JSONArray(cursor.getString(cursor.getColumnIndex("text"))));
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing text.", ex);
                throw new IllegalStateException(ex);
            }
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
            obj.put("text", CommonUtils.toJSONArray(text));
            return obj;
        }

        @NonNull
        JSONObject toSyncJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("text", text());
            obj.put("watermark", watermark);
            obj.put("type", type);
            return obj;
        }

        @NonNull
        @Override
        public String text() {
            if (sentence == null) sentence = CommonUtils.join(text, "____");
            return sentence;
        }

        @NonNull
        @Override
        public String watermark() {
            return watermark;
        }

        @Override
        public int numPick() {
            return !black() ? -1 : text.length - 1;
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
