package xyz.gianlu.pyxoverloaded.model;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.jetbrains.annotations.Contract;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import xyz.gianlu.pyxoverloaded.OverloadedChatApi;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;
import xyz.gianlu.pyxoverloaded.signal.PrefsSessionStore;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

public class EncryptedChatMessage {
    public final byte[] encrypted;
    public final int type;
    public final OverloadedUserAddress sourceAddress;
    public final long id;
    public final String from;
    public final long timestamp;

    public EncryptedChatMessage(@NonNull JSONObject obj) throws JSONException {
        id = obj.getLong("id");
        encrypted = Base64.decode(obj.getString("encrypted"), 0);
        type = obj.getInt("type");
        from = obj.getString("from");
        sourceAddress = new OverloadedUserAddress(obj.getString("sourceAddress"));
        timestamp = obj.getLong("timestamp");
    }

    @WorkerThread
    @NonNull
    public static List<PlainChatMessage> decrypt(@NonNull OverloadedChatApi api, @NonNull List<EncryptedChatMessage> encrypted) throws DecryptionException {
        List<PlainChatMessage> list = new ArrayList<>(encrypted.size());
        for (EncryptedChatMessage msg : encrypted) list.add(msg.decrypt(api));
        return list;
    }

    @Contract("_, -> new")
    @WorkerThread
    @NonNull
    public PlainChatMessage decrypt(@NonNull OverloadedChatApi api) throws DecryptionException {
        PlainChatMessage out = api.getPlainMessage(id);
        if (out != null) return out;

        if (!PrefsSessionStore.get().containsSession(sourceAddress.toSignalAddress())) {
            try {
                api.getKeySync(sourceAddress);
            } catch (ExecutionException | InterruptedException ex) {
                throw new DecryptionException("Failed obtaining keys.", ex);
            }
        }

        try {
            return SignalProtocolHelper.decrypt(this);
        } catch (InvalidVersionException | InvalidMessageException | LegacyMessageException | DuplicateMessageException | InvalidKeyIdException | UntrustedIdentityException | InvalidKeyException | NoSessionException ex) {
            throw new DecryptionException(ex);
        }
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
