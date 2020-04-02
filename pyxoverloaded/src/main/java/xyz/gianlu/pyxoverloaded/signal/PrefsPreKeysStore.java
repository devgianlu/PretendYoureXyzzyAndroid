package xyz.gianlu.pyxoverloaded.signal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PrefsPreKeysStore implements SignedPreKeyStore, PreKeyStore {
    private static PrefsPreKeysStore instance;
    private final SignalDatabaseHelper helper;

    private PrefsPreKeysStore(@NonNull SignalDatabaseHelper helper) {
        this.helper = helper;
    }

    @NonNull
    public static PrefsPreKeysStore get() {
        if (instance == null) instance = new PrefsPreKeysStore(SignalDatabaseHelper.get());
        return instance;
    }

    @Contract("_ -> new")
    @Override
    @NonNull
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM preKeys WHERE id=?", new String[]{String.valueOf(preKeyId)})) {
            if (cursor == null || !cursor.moveToNext())
                throw new InvalidKeyIdException("No such PreKeyRecord!");
            return new PreKeyRecord(cursor.getBlob(cursor.getColumnIndex("serialized")));
        } catch (IOException ex) {
            throw new AssertionError(ex);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void storePreKey(int preKeyId, @NotNull PreKeyRecord record) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(2);
            values.put("id", preKeyId);
            values.put("serialized", record.serialize());
            db.insertWithOnConflict("preKeys", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM preKeys WHERE id=?", new String[]{String.valueOf(preKeyId)})) {
            if (cursor == null || !cursor.moveToNext()) return false;
            return cursor.getInt(0) > 0;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void removePreKey(int preKeyId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("preKeys", "id=?", new String[]{String.valueOf(preKeyId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Contract("_ -> new")
    @Override
    @NonNull
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM signedPreKeys WHERE id=?", new String[]{String.valueOf(signedPreKeyId)})) {
            if (cursor == null || !cursor.moveToNext())
                throw new InvalidKeyIdException("No such SignedPreKeyRecord!");
            return new SignedPreKeyRecord(cursor.getBlob(cursor.getColumnIndex("serialized")));
        } catch (IOException ex) {
            throw new AssertionError(ex);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    @NonNull
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM signedPreKeys", null)) {
            if (cursor == null || !cursor.moveToNext()) return Collections.emptyList();

            List<SignedPreKeyRecord> list = new ArrayList<>(cursor.getCount());
            do {
                list.add(new SignedPreKeyRecord(cursor.getBlob(cursor.getColumnIndex("serialized"))));
            } while (cursor.moveToNext());
            return list;
        } catch (IOException ex) {
            throw new AssertionError(ex);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, @NotNull SignedPreKeyRecord record) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(2);
            values.put("id", signedPreKeyId);
            values.put("serialized", record.serialize());
            db.insertWithOnConflict("signedPreKeys", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM signedPreKeys WHERE id=?", new String[]{String.valueOf(signedPreKeyId)})) {
            if (cursor == null || !cursor.moveToNext()) return false;
            return cursor.getInt(0) > 0;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("signedPreKeys", "id=?", new String[]{String.valueOf(signedPreKeyId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
