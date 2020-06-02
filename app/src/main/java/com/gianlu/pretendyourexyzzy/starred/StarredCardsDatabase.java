package com.gianlu.pretendyourexyzzy.starred;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class StarredCardsDatabase extends SQLiteOpenHelper {
    private static final String TAG = StarredCardsDatabase.class.getSimpleName();
    private static StarredCardsDatabase instance = null;

    private StarredCardsDatabase(@Nullable Context context) {
        super(context, "starred_cards.db", null, 1);
    }

    @NonNull
    public static StarredCardsDatabase get(@NonNull Context context) {
        if (instance == null) instance = new StarredCardsDatabase(context);
        return instance;
    }

    @SuppressWarnings("deprecation")
    public static void migrateFromPrefs(@NonNull Context context) {
        if (!Prefs.has(PK.STARRED_CARDS)) return;

        StarredCardsDatabase db = get(context);
        try {
            JSONArray json = JsonStoring.intoPrefs().getJsonArray(PK.STARRED_CARDS);
            if (json == null) return;

            boolean ok = true;
            for (int i = 0; i < json.length(); i++) {
                StarredCard card = new StarredCard(json.getJSONObject(i));
                if (!db.putCard(card.blackCard, card.whiteCards))
                    ok = false;
            }

            if (ok) Prefs.remove(PK.STARRED_CARDS);
            Log.i(TAG, String.format("Migrated %s cards, ok: %b.", json.length(), ok));
        } catch (JSONException ex) {
            Log.e(TAG, "Failed migrating cards.", ex);
        }
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER UNIQUE PRIMARY KEY NOT NULL, blackCard TEXT NOT NULL, whiteCards TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public boolean putCard(@NonNull GameCard blackCard, @NonNull CardsGroup whiteCards) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("blackCard", blackCard.toJson().toString());
            values.put("whiteCards", whiteCards.toJson().toString());
            long id = db.insert("cards", null, values);
            db.setTransactionSuccessful();
            return id != -1;
        } catch (JSONException ex) {
            Log.e(TAG, "Failed adding card.", ex);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public void remove(@NonNull StarredCard card) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("cards", "id=?", new String[]{String.valueOf(card.id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<StarredCard> getCards() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM cards", null)) {
            if (cursor == null) return new ArrayList<>(0);

            List<StarredCard> cards = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                try {
                    cards.add(new StarredCard(cursor));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed parsing card.", ex);
                }
            }

            return cards;
        } finally {
            db.endTransaction();
        }
    }

    public boolean hasAnyCard() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM cards", null)) {
            if (cursor == null || !cursor.moveToNext()) return false;
            else return cursor.getInt(0) > 0;
        } finally {
            db.endTransaction();
        }
    }

    public static class StarredCard extends BaseCard {
        public final GameCard blackCard;
        public final CardsGroup whiteCards;
        public final int id;
        private String cachedSentence;

        private StarredCard(@NonNull Cursor cursor) throws JSONException {
            id = cursor.getInt(cursor.getColumnIndex("id"));
            blackCard = (GameCard) GameCard.parse(new JSONObject(cursor.getString(cursor.getColumnIndex("blackCard"))));
            whiteCards = CardsGroup.gameCards(new JSONArray(cursor.getString(cursor.getColumnIndex("whiteCards"))));
        }

        @Deprecated
        private StarredCard(JSONObject obj) throws JSONException {
            blackCard = (GameCard) GameCard.parse(obj.getJSONObject("bc"));
            id = obj.getInt("id");
            whiteCards = CardsGroup.gameCards(obj.getJSONArray("wc"));
        }

        @NonNull
        private String createSentence() {
            if (cachedSentence == null) {
                String blackText = blackCard.text();
                if (!blackText.contains("____"))
                    return blackText + "\n<u>" + whiteCards.get(0).text() + "</u>";

                boolean firstCapital = blackText.startsWith("____");
                for (BaseCard whiteCard : whiteCards) {
                    String whiteText = whiteCard.text();
                    if (whiteText.endsWith("."))
                        whiteText = whiteText.substring(0, whiteText.length() - 1);

                    if (firstCapital)
                        whiteText = Character.toUpperCase(whiteText.charAt(0)) + whiteText.substring(1);

                    try {
                        blackText = blackText.replaceFirst("____", "<u>" + whiteText + "</u>");
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }

                    firstCapital = false;
                }

                cachedSentence = blackText;
            }

            return cachedSentence;
        }

        @NonNull
        @Override
        public String text() {
            return createSentence();
        }

        @Override
        public String watermark() {
            return null;
        }

        @Override
        public int numPick() {
            return -1;
        }

        @Override
        public int numDraw() {
            return -1;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredCard that = (StarredCard) o;
            return id == that.id || (blackCard.equals(that.blackCard) && whiteCards.equals(that.whiteCards));
        }

        @Override
        public boolean black() {
            return false;
        }
    }
}