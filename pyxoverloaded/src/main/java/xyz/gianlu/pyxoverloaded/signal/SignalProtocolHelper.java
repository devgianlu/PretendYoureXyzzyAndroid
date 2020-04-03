package xyz.gianlu.pyxoverloaded.signal;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.preferences.Prefs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import xyz.gianlu.pyxoverloaded.model.EncryptedChatMessage;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;

public class SignalProtocolHelper {
    private static final int LOCAL_SIGNED_PRE_KEY_ID = 0;
    private static final String TAG = SignalProtocolHelper.class.getSimpleName();

    public static int getLocalDeviceId() {
        int id = Prefs.getInt(SignalPK.SIGNAL_DEVICE_ID, -1);
        if (id != -1) return id;

        id = new Random(System.currentTimeMillis()).nextInt();
        Prefs.putInt(SignalPK.SIGNAL_DEVICE_ID, id);
        Log.d(TAG, "Generated local device ID: " + id);
        return id;
    }

    @NotNull
    static IdentityKeyPair generateIdentityKeyPair() {
        IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
        Prefs.putString(SignalPK.SIGNAL_IDENTITY_KEY_PUBLIC, Base64.encodeToString(identityKeyPair.getPublicKey().serialize(), Base64.NO_WRAP));
        Prefs.putString(SignalPK.SIGNAL_IDENTITY_KEY_PRIVATE, Base64.encodeToString(identityKeyPair.getPrivateKey().serialize(), Base64.NO_WRAP));
        Log.d(TAG, "Generated local identity key: " + Base64.encodeToString(identityKeyPair.getPublicKey().serialize(), Base64.NO_WRAP));
        return identityKeyPair;
    }

    @NonNull
    public static SignedPreKeyRecord getLocalSignedPreKey() {
        SignedPreKeyRecord signedPreKey = null;
        if (PrefsPreKeysStore.get().containsSignedPreKey(LOCAL_SIGNED_PRE_KEY_ID)) {
            try {
                signedPreKey = PrefsPreKeysStore.get().loadSignedPreKey(LOCAL_SIGNED_PRE_KEY_ID);
            } catch (InvalidKeyIdException ex) {
                Log.e(TAG, "Failed loading signed pre key.", ex);
            }
        }

        if (signedPreKey == null) {
            try {
                IdentityKeyPair identityKeyPair = PrefsIdentityKeyStore.get().getIdentityKeyPair();
                signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, LOCAL_SIGNED_PRE_KEY_ID);
                PrefsPreKeysStore.get().storeSignedPreKey(LOCAL_SIGNED_PRE_KEY_ID, signedPreKey);
            } catch (InvalidKeyException ex) {
                Log.e(TAG, "Failed generating signed pre key.", ex);
                throw new IllegalStateException(ex);
            }
        }

        return signedPreKey;
    }

    @NonNull
    public static List<PreKeyRecord> generateSomePreKeys() {
        int lastPreKeyId = Prefs.getInt(SignalPK.SIGNAL_LAST_PRE_KEY_ID, -1);
        if (lastPreKeyId == -1 || lastPreKeyId == Integer.MAX_VALUE - 1)
            lastPreKeyId = 1; // ID 0 is reserved for the local key

        List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(lastPreKeyId, 50);
        Prefs.putInt(SignalPK.SIGNAL_LAST_PRE_KEY_ID, lastPreKeyId + preKeys.size());
        for (PreKeyRecord key : preKeys) PrefsPreKeysStore.get().storePreKey(key.getId(), key);
        return preKeys;
    }

    /**
     * Creates a session from scratch (to send a message)
     */
    public static void createSession(@NonNull OverloadedUserAddress address, @NonNull PreKeyBundle bundle) throws UntrustedIdentityException, InvalidKeyException {
        SessionBuilder sessionBuilder = new SessionBuilder(PrefsSessionStore.get(), PrefsPreKeysStore.get(), PrefsPreKeysStore.get(), PrefsIdentityKeyStore.get(), address.toSignalAddress());
        sessionBuilder.process(bundle);
    }

    @Contract("_ -> new")
    @NonNull
    public static PlainChatMessage decrypt(@NonNull EncryptedChatMessage ecm) throws InvalidVersionException, InvalidMessageException, LegacyMessageException, DuplicateMessageException, InvalidKeyIdException, UntrustedIdentityException, InvalidKeyException, NoSessionException {
        SessionCipher sessionCipher = new SessionCipher(PrefsSessionStore.get(), PrefsPreKeysStore.get(), PrefsPreKeysStore.get(), PrefsIdentityKeyStore.get(), ecm.sourceAddress.toSignalAddress());
        if (ecm.type == CiphertextMessage.PREKEY_TYPE) {
            PreKeySignalMessage msg = new PreKeySignalMessage(ecm.encrypted);
            return new PlainChatMessage(ecm.id, new String(sessionCipher.decrypt(msg)), ecm.timestamp, ecm.from);
        } else if (ecm.type == CiphertextMessage.WHISPER_TYPE) {
            SignalMessage msg = new SignalMessage(ecm.encrypted);
            return new PlainChatMessage(ecm.id, new String(sessionCipher.decrypt(msg)), ecm.timestamp, ecm.from);
        } else {
            throw new IllegalStateException("Unknown type: " + ecm.type);
        }
    }

    @NotNull
    public static CiphertextMessage encrypt(@NonNull OverloadedUserAddress address, @NonNull String msg) throws UntrustedIdentityException {
        return encrypt(address.toSignalAddress(), msg);
    }

    @NonNull
    private static CiphertextMessage encrypt(@NonNull SignalProtocolAddress address, @NonNull String msg) throws UntrustedIdentityException {
        SessionCipher sessionCipher = new SessionCipher(PrefsSessionStore.get(), PrefsPreKeysStore.get(), PrefsPreKeysStore.get(), PrefsIdentityKeyStore.get(), address);
        return sessionCipher.encrypt(msg.getBytes(StandardCharsets.UTF_8));
    }
}
