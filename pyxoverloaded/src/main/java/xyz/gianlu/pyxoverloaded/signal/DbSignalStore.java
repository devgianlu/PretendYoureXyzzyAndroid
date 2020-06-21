package xyz.gianlu.pyxoverloaded.signal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.Prefs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DbSignalStore implements SignalProtocolStore {
    private static final String TAG = DbSignalStore.class.getSimpleName();
    private static DbSignalStore instance;
    private final SignalDatabaseHelper helper;

    private DbSignalStore(@NonNull SignalDatabaseHelper helper) {
        this.helper = helper;
    }

    @NonNull
    public static DbSignalStore get() {
        if (instance == null) instance = new DbSignalStore(SignalDatabaseHelper.get());
        return instance;
    }

    // =========================================== //
    //              IdentityKeyStore               //
    // =========================================== //

    @Override
    @NonNull
    public IdentityKeyPair getIdentityKeyPair() {
        IdentityKey publicKey = null;
        String publicKeyStr = Prefs.getString(SignalPK.SIGNAL_IDENTITY_KEY_PUBLIC, null);
        if (publicKeyStr != null) {
            try {
                publicKey = new IdentityKey(Base64.decode(publicKeyStr, 0), 0);
            } catch (InvalidKeyException ex) {
                Log.e(TAG, "Failed parsing public identity key.", ex);
            }
        }

        if (publicKey == null)
            return SignalProtocolHelper.generateIdentityKeyPair();

        String privateKeyStr = Prefs.getString(SignalPK.SIGNAL_IDENTITY_KEY_PRIVATE, null);
        if (privateKeyStr != null) {
            ECPrivateKey privateKey = Curve.decodePrivatePoint(Base64.decode(privateKeyStr, 0));
            return new IdentityKeyPair(publicKey, privateKey);
        } else {
            return SignalProtocolHelper.generateIdentityKeyPair();
        }
    }

    @Override
    public int getLocalRegistrationId() {
        int id = Prefs.getInt(SignalPK.SIGNAL_LOCAL_REGISTRATION_ID, -1);
        if (id != -1) return id;

        id = KeyHelper.generateRegistrationId(false);
        Prefs.putInt(SignalPK.SIGNAL_LOCAL_REGISTRATION_ID, id);
        Prefs.putBoolean(SignalPK.SIGNAL_UPDATE_KEYS, true);
        Log.d(TAG, "Generated registration ID: " + id);
        return id;
    }

    @Override
    public boolean saveIdentity(@NotNull SignalProtocolAddress address, @NotNull IdentityKey identityKey) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(3);
            values.put("name", address.getName());
            values.put("deviceId", address.getDeviceId());
            values.put("serialized", identityKey.serialize());

            long result = db.insertWithOnConflict("identityKeys", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            return result != -1;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, @NotNull IdentityKey identityKey, Direction direction) {
        return true;
    }

    @Override
    @Nullable
    public IdentityKey getIdentity(SignalProtocolAddress address) {
        SQLiteDatabase db = helper.getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM identityKeys WHERE name=? AND deviceId=?", new String[]{address.getName(), String.valueOf(address.getDeviceId())})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            return new IdentityKey(cursor.getBlob(cursor.getColumnIndex("serialized")), 0);
        } catch (InvalidKeyException ex) {
            throw new AssertionError(ex);
        } finally {
            db.endTransaction();
        }
    }

    // =========================================== //
    //                SessionStore                 //
    // =========================================== //

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

    // =========================================== //
    //                 PreKeyStore                 //
    // =========================================== //

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

    // =========================================== //
    //              SignedPreKeyStore              //
    // =========================================== //

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
