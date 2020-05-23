package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CustomDecksDatabase extends SQLiteOpenHelper {
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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

    public static class CustomDeck {
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
    }
}
