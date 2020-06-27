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
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.StarredCardsPatchOp;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.StarredCardsUpdateResponse;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

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

    public static long getRevision() {
        return Prefs.getLong(PK.STARRED_CARDS_REVISION, 0);
    }

    public static void setRevision(long revision) {
        Prefs.putLong(PK.STARRED_CARDS_REVISION, revision);
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
                JSONObject obj = json.getJSONObject(i);
                GameCard black = (GameCard) GameCard.parse(obj.getJSONObject("bc"));
                CardsGroup whites = CardsGroup.gameCards(obj.getJSONArray("wc"));
                if (whites.isEmpty()) {
                    ok = false;
                    continue;
                }

                if (!db.putCard(black, whites))
                    ok = false;
            }

            if (ok) Prefs.remove(PK.STARRED_CARDS);
            Log.i(TAG, String.format("Migrated %s cards, ok: %b.", json.length(), ok));
        } catch (JSONException ex) {
            Log.e(TAG, "Failed migrating cards.", ex);
        }
    }

    @NonNull
    public static List<FloatingStarredCard> transform(@NonNull List<UserProfile.StarredCard> original) {
        List<FloatingStarredCard> list = new ArrayList<>(original.size());
        for (UserProfile.StarredCard card : original) list.add(new FloatingStarredCard(card));
        return list;
    }

    @NonNull
    private static String createSentence(@NonNull String blackText, @NonNull String[] whiteTexts) {
        if (!blackText.contains("____")) {
            StringBuilder builder = new StringBuilder(blackText);
            for (String whiteText : whiteTexts)
                builder.append("<br><u>").append(whiteText).append("</u>");
            return builder.toString();
        }

        boolean firstCapital = blackText.startsWith("____");
        StringBuilder builder = new StringBuilder(blackText);
        for (String whiteText : whiteTexts) {
            if (whiteText.endsWith("."))
                whiteText = whiteText.substring(0, whiteText.length() - 1);

            if (firstCapital) {
                whiteText = Character.toUpperCase(whiteText.charAt(0)) + whiteText.substring(1);
                firstCapital = false;
            }

            int index;
            if ((index = builder.indexOf("____")) != -1)
                builder.replace(index, index + 4, "<u>" + whiteText + "</u>");
            else
                builder.append("<br><u>").append(whiteText).append("</u>");
        }

        return builder.toString();
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER UNIQUE PRIMARY KEY NOT NULL, blackCard TEXT NOT NULL, whiteCards TEXT NOT NULL, remoteId INTEGER UNIQUE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void sendPatch(@NonNull StarredCardsPatchOp op, long localId, @Nullable Long remoteId, @Nullable ContentCard blackCard, @Nullable CardsGroup whiteCards) throws JSONException {
        if (!OverloadedApi.get().isFullyRegistered())
            return;

        JSONObject obj;
        if (op == StarredCardsPatchOp.REM && remoteId != null) {
            obj = null;
        } else if (op == StarredCardsPatchOp.ADD && blackCard != null && whiteCards != null) {
            obj = new JSONObject();
            obj.put("bc", blackCard.toJson());
            obj.put("wc", ContentCard.toJson(whiteCards));
        } else {
            throw new IllegalArgumentException();
        }

        OverloadedSyncApi.get().patchStarredCards(getRevision(), op, remoteId, obj, null, new GeneralCallback<StarredCardsUpdateResponse>() {
            @Override
            public void onResult(@NonNull StarredCardsUpdateResponse result) {
                Log.i(TAG, "Performed patch on server: " + getRevision());
                if (op == StarredCardsPatchOp.ADD && result.remoteId != null)
                    setRemoteId(localId, result.remoteId);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed performing patch on server: " + op, ex);
            }
        });
    }

    public void setRemoteId(long localId, long remoteId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("remoteId", remoteId);
            db.update("cards", values, "id=?", new String[]{String.valueOf(localId)});
        } finally {
            db.endTransaction();
        }
    }

    public boolean putCard(@NonNull ContentCard blackCard, @NonNull CardsGroup whiteCards) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("blackCard", blackCard.toJson().toString());
            values.put("whiteCards", ContentCard.toJson(whiteCards).toString());
            long id = db.insert("cards", null, values);
            db.setTransactionSuccessful();
            setRevision(OverloadedApi.now());

            try {
                sendPatch(StarredCardsPatchOp.ADD, id, null, blackCard, whiteCards);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed sending add patch.", ex);
            }

            return id != -1;
        } catch (JSONException ex) {
            Log.e(TAG, "Failed adding card.", ex);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean putCard(@NonNull GameCard blackCard, @NonNull CardsGroup whiteCards) {
        return putCard(ContentCard.from(blackCard), whiteCards);
    }

    public boolean putCard(@NonNull FloatingStarredCard card) {
        return putCard(ContentCard.from(card.card.blackCard), ContentCard.from(card.card.whiteCards));
    }

    public void remove(@NonNull StarredCard card) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("cards", "id=?", new String[]{String.valueOf(card.id)});
            db.setTransactionSuccessful();
            setRevision(OverloadedApi.now());

            try {
                if (card.remoteId != null)
                    sendPatch(StarredCardsPatchOp.REM, card.id, card.remoteId, null, null);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed sending remove patch.", ex);
            }
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<StarredCard> getCards(boolean leftover) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery(leftover ? "SELECT * FROM cards WHERE remoteId IS NULL" : "SELECT * FROM cards", null)) {
            if (cursor == null) return new ArrayList<>(0);

            List<StarredCard> cards = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                try {
                    cards.add(new StarredCard(cursor));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed parsing card.", ex);
                }
            }

            Collections.reverse(cards);
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

    @NonNull
    public UpdatePair getUpdate() {
        JSONArray array = new JSONArray();
        List<StarredCard> list = getCards(false);
        long[] localIds = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            StarredCard card = list.get(i);
            localIds[i] = card.id;

            try {
                JSONObject obj = new JSONObject();
                obj.put("remoteId", card.remoteId);
                obj.put("bc", card.blackCard.toJson());
                obj.put("wc", ContentCard.toJson(card.whiteCards));
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }

        return new UpdatePair(array, localIds);
    }

    public void loadUpdate(@NonNull JSONArray update, boolean delete, @Nullable Long revision) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (delete) db.execSQL("DELETE FROM cards WHERE remoteId IS NOT NULL");

            for (int i = 0; i < update.length(); i++) {
                JSONObject obj = update.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put("blackCard", obj.getJSONObject("bc").toString());
                values.put("whiteCards", obj.getJSONArray("wc").toString());
                values.put("remoteId", obj.getLong("remoteId"));
                db.insert("cards", null, values);
            }

            db.setTransactionSuccessful();
            if (revision != null) setRevision(revision);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed adding card.", ex);
        } finally {
            db.endTransaction();
        }
    }

    public static class FloatingStarredCard extends BaseCard {
        private final UserProfile.StarredCard card;
        private transient String cachedSentence;

        private FloatingStarredCard(@NonNull UserProfile.StarredCard card) {
            this.card = card;
        }

        @NonNull
        @Override
        public String text() {
            if (cachedSentence == null) {
                String[] whiteTexts = new String[card.whiteCards.length];
                for (int i = 0; i < whiteTexts.length; i++) whiteTexts[i] = card.whiteCards[i].text;
                cachedSentence = createSentence(card.blackCard.text, whiteTexts);
            }

            return cachedSentence;
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
        public boolean black() {
            return false;
        }
    }

    public static class StarredCard extends BaseCard {
        public final ContentCard blackCard;
        public final CardsGroup whiteCards;
        public final int id;
        private final Long remoteId;
        private transient String cachedSentence;

        private StarredCard(@NonNull Cursor cursor) throws JSONException {
            id = cursor.getInt(cursor.getColumnIndex("id"));
            blackCard = ContentCard.parse(new JSONObject(cursor.getString(cursor.getColumnIndex("blackCard"))), true);
            whiteCards = ContentCard.parse(new JSONArray(cursor.getString(cursor.getColumnIndex("whiteCards"))), false);

            int remoteIdIndex = cursor.getColumnIndex("remoteId");
            if (cursor.isNull(remoteIdIndex)) remoteId = null;
            else remoteId = cursor.getLong(remoteIdIndex);
        }

        @NonNull
        @Override
        public String text() {
            if (cachedSentence == null) {
                String[] whiteTexts = new String[whiteCards.size()];
                for (int i = 0; i < whiteCards.size(); i++)
                    whiteTexts[i] = whiteCards.get(i).text();
                cachedSentence = createSentence(blackCard.text(), whiteTexts);
            }

            return cachedSentence;
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

    public static class UpdatePair {
        public final JSONArray update;
        public final long[] localIds;

        private UpdatePair(@NonNull JSONArray array, @NonNull long[] localIds) {
            this.update = array;
            this.localIds = localIds;
        }
    }
}
