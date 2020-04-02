package xyz.gianlu.pyxoverloaded.signal;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.Prefs;

import org.jetbrains.annotations.NotNull;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;


public final class PrefsIdentityKeyStore implements IdentityKeyStore {
    private static final String TAG = PrefsIdentityKeyStore.class.getSimpleName();
    private static PrefsIdentityKeyStore instance;
    private final SignalDatabaseHelper helper;

    private PrefsIdentityKeyStore(@NonNull SignalDatabaseHelper helper) {
        this.helper = helper;
    }

    @NonNull
    public static PrefsIdentityKeyStore get() {
        if (instance == null) instance = new PrefsIdentityKeyStore(SignalDatabaseHelper.get());
        return instance;
    }

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

            long result = db.insertWithOnConflict("identityKeys", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful();
            return result != -1;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, @NotNull IdentityKey identityKey, Direction direction) {
        IdentityKey trusted = getIdentity(address);
        return trusted == null || trusted.equals(identityKey);
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
}
