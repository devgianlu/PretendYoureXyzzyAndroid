package xyz.gianlu.pyxoverloaded.signal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PrefsSessionStore implements SessionStore {
    private static PrefsSessionStore instance;
    private final SignalDatabaseHelper helper;

    private PrefsSessionStore(@NonNull SignalDatabaseHelper helper) {
        this.helper = helper;
    }

    @NonNull
    public static PrefsSessionStore get() {
        if (instance == null) instance = new PrefsSessionStore(SignalDatabaseHelper.get());
        return instance;
    }

    @Contract("_ -> new")
    @Override
    @NonNull
    public SessionRecord loadSession(SignalProtocolAddress address) {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM sessions WHERE name=? AND deviceId=?", new String[]{address.getName(), String.valueOf(address.getDeviceId())})) {
            if (cursor == null || !cursor.moveToNext()) return new SessionRecord();
            return new SessionRecord(cursor.getBlob(cursor.getColumnIndex("serialized")));
        } catch (IOException ex) {
            throw new AssertionError(ex);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    @NonNull
    public List<Integer> getSubDeviceSessions(String name) {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT DISTINCT deviceId FROM sessions WHERE name=?", new String[]{name})) {
            if (cursor == null || !cursor.moveToNext()) return Collections.emptyList();

            Set<Integer> list = new HashSet<>(cursor.getCount());
            do {
                list.add(cursor.getInt(0));
            } while (cursor.moveToNext());
            return new ArrayList<>(list);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void storeSession(@NotNull SignalProtocolAddress address, @NotNull SessionRecord record) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("name", address.getName());
            values.put("deviceId", address.getDeviceId());
            values.put("serialized", record.serialize());
            db.insertWithOnConflict("sessions", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sessions WHERE name=? AND deviceId=?", new String[]{address.getName(), String.valueOf(address.getDeviceId())})) {
            if (cursor == null || !cursor.moveToNext()) return false;
            return cursor.getInt(0) > 0;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteSession(@NotNull SignalProtocolAddress address) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("sessions", "name=? AND deviceId=?", new String[]{address.getName(), String.valueOf(address.getDeviceId())});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteAllSessions(String name) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("sessions", "name=?", new String[]{name});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
