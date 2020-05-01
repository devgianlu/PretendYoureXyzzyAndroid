package xyz.gianlu.pyxoverloaded.model;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.List;
import java.util.concurrent.ExecutionException;

import xyz.gianlu.pyxoverloaded.OverloadedChatApi;
import xyz.gianlu.pyxoverloaded.signal.DbSignalStore;
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
    public static void decrypt(@NonNull OverloadedChatApi api, int chatId, @NonNull List<EncryptedChatMessage> encrypted) {
        for (EncryptedChatMessage msg : encrypted) {
            try {
                msg.decrypt(api, chatId);
            } catch (DecryptionException ex) {
                Log.e(TAG, String.format("Failed decrypting message %s.", msg), ex);
            }
        }
    }

    @WorkerThread
    @NonNull
    public PlainChatMessage decrypt(@NonNull OverloadedChatApi api, int chatId) throws DecryptionException {
        if (!DbSignalStore.get().containsSession(sourceAddress.toSignalAddress())) {
            try {
                api.getKeySync(sourceAddress);
                Log.d(TAG, "Retrieved key for " + sourceAddress);
            } catch (ExecutionException | InterruptedException ex) {
                throw new DecryptionException("Failed obtaining keys.", ex);
            }
        }

        String text;
        try {
            text = SignalProtocolHelper.decrypt(this);
        } catch (InvalidVersionException | InvalidMessageException | LegacyMessageException | DuplicateMessageException | InvalidKeyIdException | UntrustedIdentityException | InvalidKeyException | NoSessionException ex) {
            throw new DecryptionException(ex);
        }

        PlainChatMessage msg = api.db.putMessage(chatId, text, timestamp, from);
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

        DecryptionException(@Nullable String message, @Nullable Throwable cause) {
            super(message, cause);
        }
    }
}
