package xyz.gianlu.pyxoverloaded;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.state.PreKeyRecord;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import xyz.gianlu.pyxoverloaded.OverloadedApi.Event;
import xyz.gianlu.pyxoverloaded.OverloadedApi.OverloadedServerException;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.EncryptedChatMessage;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.signal.DbSignalStore;
import xyz.gianlu.pyxoverloaded.signal.OutgoingMessageEnvelope;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;
import xyz.gianlu.pyxoverloaded.signal.SignalPK;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

import static xyz.gianlu.pyxoverloaded.TaskUtils.loggingCallbacks;

public class OverloadedChatApi implements Closeable {
    private static final String TAG = OverloadedApi.class.getSimpleName();
    public final ChatDatabaseHelper db;
    private final OverloadedApi api;
    private final List<UnreadCountListener> unreadCountListeners = Collections.synchronizedList(new ArrayList<>());

    OverloadedChatApi(@NonNull Context context, @NonNull OverloadedApi api) {
        this.api = api;
        this.db = new ChatDatabaseHelper(context);
    }

    //region Keys
    /**
     * Sends keys (without generating pre-keys) to the server if needed.
     */
    public void shareKeysIfNeeded() {
        if (Prefs.getBoolean(SignalPK.SIGNAL_UPDATE_KEYS, false))
            shareKeys(false);
    }

    /**
     * Collects the necessary keys and sends them to the server.
     */
    void shareKeys(boolean includePreKeys) {
        loggingCallbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("registrationId", DbSignalStore.get().getLocalRegistrationId());
            body.put("identityKey", Base64.encodeToString(DbSignalStore.get().getIdentityKeyPair().getPublicKey().serialize(), Base64.NO_WRAP));
            body.put("signedPreKey", Utils.toServerJson(SignalProtocolHelper.getLocalSignedPreKey()));

            JSONArray preKeysArray = new JSONArray();
            if (includePreKeys) {
                List<PreKeyRecord> preKeys = SignalProtocolHelper.generateSomePreKeys();
                for (PreKeyRecord key : preKeys) preKeysArray.put(Utils.toServerJson(key));
            }
            body.put("preKeys", preKeysArray);

            api.makePostRequest("Chat/ShareKeys", body);
            Prefs.putBoolean(SignalPK.SIGNAL_UPDATE_KEYS, false);
            return null;
        }), "share-pre-keys");
    }

    /**
     * Gets a pre key bundle from the server <b>synchronously</b> and creates a new session.
     *
     * @param address The {@link OverloadedUserAddress} of the key
     */
    @WorkerThread
    private void getKeySync(@NonNull OverloadedUserAddress address) throws ExecutionException, InterruptedException {
        Tasks.await(loggingCallbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("address", address.uid);
            body.put("deviceId", address.deviceId);

            JSONObject obj = api.makePostRequest("Chat/GetKeys", body);

            JSONObject keyObj = obj.getJSONObject("key");
            SignalProtocolHelper.createSession(new OverloadedUserAddress(keyObj.getString("address")), Utils.parsePreKeyBundle(keyObj));
            return null;
        }), "get-keys-" + address.toString()));
    }
    //endregion

    //region Chats
    /**
     * Starts a chat with the specified user.
     *
     * @param username The recipient username
     * @return A task resolving to the {@link Chat} object.
     */
    @NonNull
    public Task<Chat> startChat(@NonNull String username) {
        Chat chat = db.findChatWith(username);
        if (chat != null) Tasks.forResult(chat);

        return Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("username", username);
            JSONObject obj = api.makePostRequest("Chat/Start", body);
            return db.putChat(obj.getString("address"), username);
        });
    }

    /**
     * Gets a list of all the user's chats
     *
     * @return A list of {@link Chat}s
     */
    @NonNull
    public List<Chat> listChats() {
        return db.getChats();
    }

    /**
     * Deletes all the local messages for this chat.
     *
     * @param chat The {@link Chat} to delete
     */
    public void deleteChat(@NonNull Chat chat) {
        db.removeChat(chat);
        dispatchUnreadCountUpdate();
    }

    /**
     * Gets a chat for the given chat ID.
     *
     * @param chatId The chat ID
     * @return The {@link Chat} object or {@code null}
     */
    @Nullable
    public Chat getChat(int chatId) {
        return db.getChat(chatId);
    }
    //endregion

    //region Sending

    /**
     * Sends a message on the specified chat after encrypting it.
     *
     * @param chatId The chat ID
     * @param text   The plaintext message
     * @return A task resolving to the {@link PlainChatMessage}
     */
    @NotNull
    public Task<PlainChatMessage> sendMessage(int chatId, @NonNull String text) {
        return api.userData(true).continueWith(api.executorService, (task) -> {
            UserData data = task.getResult();
            if (data == null) throw new IllegalStateException();

            Chat chat = db.getChat(chatId);
            if (chat == null) throw new IllegalStateException();

            Exception lastEx = null;
            for (int i = 0; i < 4; i++) {
                List<OutgoingMessageEnvelope> outgoing = SignalProtocolHelper.encrypt(chat, text);
                JSONArray encryptedMessages = new JSONArray();
                for (OutgoingMessageEnvelope msg : outgoing) encryptedMessages.put(msg.toJson());

                JSONObject body = new JSONObject();
                body.put("address", chat.address);
                body.put("messages", encryptedMessages);

                try {
                    api.makePostRequest("Chat/Send", body);
                    return db.putMessage(chatId, text, OverloadedApi.now(), data.username);
                } catch (OverloadedServerException ex) {
                    lastEx = ex;

                    if (ex.reason.equals(OverloadedServerException.REASON_MISMATCHED_DEVICES) && ex.details != null) {
                        if (!handleMismatchedDevices(chat, ex.details))
                            throw ex;
                    } else if (ex.reason.equals(OverloadedServerException.REASON_STALE_DEVICES) && ex.details != null) {
                        if (!handleStaleDevices(chat, ex.details))
                            throw ex;
                    } else {
                        throw ex;
                    }
                }
            }

            throw lastEx;
        }).continueWith(task -> {
            PlainChatMessage msg = task.getResult();
            dispatchDecryptedMessage(msg);
            return msg;
        });
    }

    @WorkerThread
    private boolean handleStaleDevices(@NonNull Chat chat, @NonNull JSONObject obj) throws JSONException {
        JSONArray staleDevices = obj.optJSONArray("stale");
        if (staleDevices == null) return false;

        for (int i = 0; i < staleDevices.length(); i++)
            DbSignalStore.get().deleteSession(chat.deviceAddress(staleDevices.getInt(i)).toSignalAddress());

        return true;
    }

    @WorkerThread
    private boolean handleMismatchedDevices(@NonNull Chat chat, @NonNull JSONObject obj) throws JSONException, InterruptedException {
        JSONArray extra = obj.optJSONArray("extra");
        if (extra == null) return false;

        for (int i = 0; i < extra.length(); i++)
            DbSignalStore.get().deleteSession(chat.deviceAddress(extra.getInt(i)).toSignalAddress());

        JSONArray missing = obj.optJSONArray("missing");
        if (missing == null) return false;

        for (int i = 0; i < missing.length(); i++) {
            OverloadedUserAddress address = chat.deviceAddress(missing.getInt(i));

            try {
                getKeySync(address);
            } catch (ExecutionException ex) {
                Log.e(TAG, "Failed getting prekey for " + address, ex);
            }
        }

        return true;
    }
    //endregion

    //region Local messages
    /**
     * Gets a list of locally stored messages (128 max) from the given time.
     *
     * @param chatId The chat ID
     * @param since  The lower time bound
     * @return A list of chat messages
     */
    @Nullable
    public List<PlainChatMessage> getLocalMessages(int chatId, long since) {
        return db.getMessagesPaginate(chatId, since);
    }

    /**
     * Gets a list of locally stored messages (128 most recent ones).
     *
     * @param chatId The chat ID
     * @return A list of chat messages
     */
    @NonNull
    public List<PlainChatMessage> getLocalMessages(int chatId) {
        List<PlainChatMessage> msg = db.getMessages(chatId);
        return msg == null ? Collections.emptyList() : msg;
    }
    //endregion

    //region Last message
    /**
     * Gets the last message of the given chat.
     *
     * @param chatId The chat ID
     * @return A {@link PlainChatMessage} or {@code null} if the chat is empty
     */
    @Nullable
    public PlainChatMessage getLastMessage(int chatId) {
        return db.getLastMessage(chatId);
    }

    /**
     * Updates the last seen message for the given chat.
     *
     * @param chatId The chat ID
     * @param msg    The last seen message
     */
    public void updateLastSeen(int chatId, @NonNull PlainChatMessage msg) {
        db.updateLastSeen(chatId, msg.timestamp + 1);
        dispatchUnreadCountUpdate();
    }

    /**
     * Counts the number of unread messages for the given chat.
     *
     * @param chatId The chat ID
     * @return The count of unread messages
     */
    public int countSinceLastSeen(int chatId) {
        return db.countSinceLastSeen(chatId);
    }
    //endregion

    //region Unread
    private void dispatchUnreadCountUpdate() {
        synchronized (unreadCountListeners) {
            Handler handler = new Handler(Looper.getMainLooper());
            for (UnreadCountListener listener : unreadCountListeners)
                handler.post(listener::mayUpdateUnreadCount);
        }
    }

    /**
     * @return The total number of unread messages across chats
     */
    public int countTotalUnread() {
        return db.countTotalUnread();
    }

    public void addUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.add(listener);
    }

    public void removeUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.remove(listener);
    }
    //endregion

    //region Receive and decrypt
    /**
     * Sends an acknowledgment to the server (that it has received the message).
     *
     * @param ackId The ack ID
     */
    public void decryptAck(long ackId) {
        loggingCallbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("ackId", ackId);
            api.makePostRequest("Chat/Ack", body);
            return null;
        }), "message-ack: " + ackId);
    }

    @WorkerThread
    private void dispatchDecryptedMessage(@NonNull PlainChatMessage msg) {
        api.dispatchLocalEvent(Event.Type.CHAT_MESSAGE, msg);
    }

    @WorkerThread
    void handleEvent(@NonNull Event event) throws JSONException {
        if (event.type == Event.Type.ENCRYPTED_CHAT_MESSAGE && event.data != null) {
            PlainChatMessage msg;
            try {
                msg = new EncryptedChatMessage(event.data).decrypt(this);
            } catch (EncryptedChatMessage.DecryptionException ex) {
                Log.e(TAG, "Failed decrypting message.", ex);
                return;
            }

            dispatchUnreadCountUpdate();
            dispatchDecryptedMessage(msg);
        } else if (event.type == Event.Type.SHARE_KEYS_LOW) {
            shareKeys(true);
        }
    }
    //endregion

    @Override
    public void close() {
        db.close();
    }

    @UiThread
    public interface UnreadCountListener {
        void mayUpdateUnreadCount();
    }
}
