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
        super(context, "custom_decks.db", null, 1);
    }

    @NonNull
    public static CustomDecksDatabase get(@NonNull Context context) {
        if (instance == null) instance = new CustomDecksDatabase(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS decks (id INTEGER PRIMARY KEY UNIQUE, name TEXT NOT NULL UNIQUE, watermark TEXT NOT NULL, description TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY UNIQUE, deck_id INTEGER NOT NULL, type INTEGER NOT NULL, text TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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
            int id = (int) db.insert("decks", null, values);
            db.setTransactionSuccessful();
            if (id == -1) return null;
            else return new CustomDeck(id, name, watermark, desc);
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
            db.update("decks", values, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<CustomCard> loadCards(int deckId, int type) {
        CustomDeck deck = getDeck(deckId);
        if (deck == null) throw new IllegalStateException();

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM cards WHERE type=? AND deck_id=?", new String[]{String.valueOf(type), String.valueOf(deckId)})) {
            if (cursor == null) return new ArrayList<>(0);

            List<CustomCard> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                list.add(new CustomCard(deck, cursor));
            }
            return list;
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<CustomCard> loadBlackCards(int deckId) {
        return loadCards(deckId, CARD_TYPE_BLACK);
    }

    @NonNull
    public List<CustomCard> loadWhiteCards(int deckId) {
        return loadCards(deckId, CARD_TYPE_WHITE);
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
            db.setTransactionSuccessful();

            if (id == -1) return null;
            else return new CustomCard(text, deck.watermark, type, (int) id);
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public CustomCard updateCard(@NonNull CustomCard old, @NonNull String[] text) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("text", CommonUtils.toJSONArray(text).toString());
            db.update("cards", values, "id=?", new String[]{String.valueOf(old.id())});
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

    public static final class CustomDeck {
        public final int id;
        public final String name;
        public final String watermark;
        public final String description;

        private CustomDeck(@NonNull Cursor cursor) {
            id = cursor.getInt(cursor.getColumnIndex("id"));
            name = cursor.getString(cursor.getColumnIndex("name"));
            watermark = cursor.getString(cursor.getColumnIndex("watermark"));
            description = cursor.getString(cursor.getColumnIndex("description"));
        }

        private CustomDeck(int id, @NonNull String name, @NonNull String watermark, @NonNull String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.watermark = watermark;
        }

        @NonNull
        public JSONObject craftJson(@NonNull CustomDecksDatabase db) throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("description", description);
            obj.put("watermark", watermark);

            List<CustomCard> calls = db.loadBlackCards(id);
            JSONArray callsArray = new JSONArray();
            for (CustomCard card : calls) callsArray.put(card.craftJson());
            obj.put("calls", callsArray);

            List<CustomCard> responses = db.loadWhiteCards(id);
            JSONArray responsesArray = new JSONArray();
            for (CustomCard card : responses) responsesArray.put(card.craftJson());
            obj.put("responses", responsesArray);

            return obj;
        }
    }

    public static final class CustomCard extends BaseCard {
        private final String[] text;
        private final String watermark;
        private final int type;
        private final int id;
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
        @Override
        public String text() {
            if (sentence == null) sentence = CommonUtils.join(text, " ____ ");
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
        public int id() {
            return id;
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
