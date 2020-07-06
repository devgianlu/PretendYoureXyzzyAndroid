package xyz.gianlu.pyxoverloaded.model;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;

import xyz.gianlu.pyxoverloaded.OverloadedChatApi;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

public class EncryptedChatMessage {
    private static final String TAG = EncryptedChatMessage.class.getSimpleName();
    public final byte[] encrypted;
    public final int type;
    public final OverloadedUserAddress sourceAddress;
    public final String from;
    public final long timestamp;
    private final long ackId;

    public EncryptedChatMessage(@NonNull JSONObject obj) throws JSONException {
        encrypted = Base64.decode(obj.getString("encrypted"), 0);
        type = obj.getInt("type");
        from = obj.getString("from");
        sourceAddress = new OverloadedUserAddress(obj.getString("sourceAddress"));
        timestamp = obj.getLong("timestamp");
        ackId = obj.getLong("ackId");
    }

    @NonNull
    public static String typeToString(int type) {
        switch (type) {
            case CiphertextMessage.WHISPER_TYPE:
                return "WHISPER";
            case CiphertextMessage.PREKEY_TYPE:
                return "PREKEY";
            case CiphertextMessage.SENDERKEY_TYPE:
                return "SENDERKEY";
            case CiphertextMessage.SENDERKEY_DISTRIBUTION_TYPE:
                return "SENDERKEY_DISTRIBUTION";
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    @WorkerThread
    @NonNull
    public PlainChatMessage decrypt(@NonNull OverloadedChatApi api) throws DecryptionException {
        String text;
        try {
            text = SignalProtocolHelper.decrypt(this);
        } catch (InvalidVersionException | InvalidMessageException | LegacyMessageException | InvalidKeyIdException | UntrustedIdentityException | InvalidKeyException | NoSessionException ex) {
            throw new DecryptionException(ex);
        } catch (DuplicateMessageException ex) {
            Log.w(TAG, "Received duplicate message. Acknowledging anyway. " + this, ex);
            api.decryptAck(ackId);
            throw new DecryptionException(ex);
        }

        PlainChatMessage msg = api.db.putMessage(text, timestamp, sourceAddress.uid, from);
        api.decryptAck(ackId);
        return msg;
    }

    @NotNull
    @Override
    public String toString() {
        return "EncryptedChatMessage{type=" + typeToString(type) + ", sourceAddress=" + sourceAddress + ", from='" + from + '\'' + ", timestamp=" + timestamp + '}';
    }

    public static class DecryptionException extends Throwable {
        DecryptionException(@NonNull Throwable cause) {
            super(cause);
        }
    }
}
