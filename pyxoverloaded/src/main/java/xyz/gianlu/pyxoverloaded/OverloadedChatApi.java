package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.state.PreKeyRecord;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okhttp3.Request;
import okhttp3.internal.Util;
import xyz.gianlu.pyxoverloaded.callback.ChatCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatMessageCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatsCallback;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.EncryptedChatMessage;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.signal.DbSignalStore;
import xyz.gianlu.pyxoverloaded.signal.OutgoingMessageEnvelope;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

import static xyz.gianlu.pyxoverloaded.TaskUtils.callbacks;
import static xyz.gianlu.pyxoverloaded.TaskUtils.loggingCallbacks;
import static xyz.gianlu.pyxoverloaded.Utils.jsonBody;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;

public class OverloadedChatApi implements Closeable {
    private static final String TAG = OverloadedApi.class.getSimpleName();
    public final ChatDatabaseHelper db;
    private final OverloadedApi api;
    private final List<UnreadCountListener> unreadCountListeners = Collections.synchronizedList(new ArrayList<>());

    OverloadedChatApi(@NonNull Context context, @NonNull OverloadedApi api) {
        this.api = api;
        this.db = new ChatDatabaseHelper(context);
    }

    ///////////////////////////////
    //////////// Keys /////////////
    ///////////////////////////////

    /**
     * Collects the necessary keys and sends them to the server.
     */
    void sharePreKeys() {
        loggingCallbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("registrationId", DbSignalStore.get().getLocalRegistrationId());
            body.put("identityKey", Base64.encodeToString(DbSignalStore.get().getIdentityKeyPair().getPublicKey().serialize(), Base64.NO_WRAP));
            body.put("signedPreKey", Utils.toServerJson(SignalProtocolHelper.getLocalSignedPreKey()));

            JSONArray preKeysArray = new JSONArray();
            List<PreKeyRecord> preKeys = SignalProtocolHelper.generateSomePreKeys();
            for (PreKeyRecord key : preKeys) preKeysArray.put(Utils.toServerJson(key));
            body.put("preKeys", preKeysArray);

            api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/ShareKeys"))
                    .post(jsonBody(body)));
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

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/GetKeys"))
                    .post(jsonBody(body)));

            JSONObject keyObj = obj.getJSONObject("key");
            SignalProtocolHelper.createSession(new OverloadedUserAddress(keyObj.getString("address")), Utils.parsePreKeyBundle(keyObj));
            return null;
        }), "get-keys-" + address.toString()));
    }


    ///////////////////////////////
    //////////// Chats ////////////
    ///////////////////////////////

    /**
     * Gets a summary of unread messages (and related chats), also checks if the server needs some keys.
     *
     * @return The ongoing {@link Task}
     */
    @NonNull
    public Task<Integer> getSummary() {
        return loggingCallbacks(Tasks.call(api.executorService, () -> {
            Long since = db.getLastLastSeen();
            JSONObject body = new JSONObject();
            body.put("since", since == null ? 0 : since);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Summary"))
                    .post(jsonBody(body)));

            if (obj.optBoolean("shareKeys", false))
                sharePreKeys();

            JSONArray summary = obj.getJSONArray("summary");
            for (int i = 0; i < summary.length(); i++) {
                JSONObject chatObj = summary.getJSONObject(i);

                int chatId = chatObj.getInt("chatId");
                Chat chat = db.getChat(chatId);
                if (chat == null) {
                    chat = new Chat(chatObj.getJSONObject("chat"));
                    db.putChat(chat);
                }

                JSONArray messagesArray = chatObj.getJSONArray("messages");
                List<EncryptedChatMessage> encryptedMessages = new ArrayList<>(messagesArray.length());
                for (int j = 0; j < messagesArray.length(); j++)
                    encryptedMessages.add(new EncryptedChatMessage(messagesArray.getJSONObject(j)));

                List<PlainChatMessage> messages = EncryptedChatMessage.decrypt(OverloadedChatApi.this, chatId, encryptedMessages);
                for (PlainChatMessage msg : messages) dispatchDecryptedMessage(msg);
            }

            return summary.length();
        }), "chat-summary");
    }

    /**
     * Starts a chat with the specified user.
     *
     * @param username The recipient username
     * @param activity The caller {@link Activity}
     * @param callback The callback containing the chat info
     */
    public void startChat(@NonNull String username, @Nullable Activity activity, @NonNull ChatCallback callback) {
        Chat chat = db.findChatWith(username);
        if (chat != null)
            new Handler(Looper.getMainLooper()).post(() -> callback.onChat(chat));

        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("username", username);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Start"))
                    .post(jsonBody(body)));

            Chat remoteChat = new Chat(obj);
            db.putChat(remoteChat);
            return remoteChat;
        }), activity, remoteChat -> {
            if (chat == null) callback.onChat(remoteChat);
        }, callback::onFailed);
    }

    /**
     * Gets a list of all the user's chats
     *
     * @param activity The caller {@link Activity}
     * @param callback The callback containing all the chats
     */
    public void listChats(@Nullable Activity activity, @NonNull ChatsCallback callback) {
        List<Chat> list = db.getChats();
        if (!list.isEmpty())
            new Handler(Looper.getMainLooper()).post(() -> callback.onLocalChats(list));

        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/List"))
                    .post(Util.EMPTY_REQUEST));

            List<Chat> local = new ArrayList<>(list);
            JSONArray array = obj.getJSONArray("chats");
            List<Chat> chats = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject chatObj = array.getJSONObject(i);
                Chat chat = new Chat(chatObj);
                chats.add(chat);
                db.putChat(chat);
                local.remove(chat);
            }

            for (Chat chat : local) db.removeChat(chat);

            return chats;
        }), activity, callback::onRemoteChats, callback::onFailed);
    }


    ///////////////////////////////
    /////////// Sending ///////////
    ///////////////////////////////

    /**
     * Sends a message on the specified chat after encrypting it.
     *
     * @param chatId   The chat ID
     * @param text     The plaintext message
     * @param activity The caller {@link Activity}
     * @param callback The callback containing the decrypted message (for local dispatching)
     */
    public void sendMessage(int chatId, @NonNull String text, @Nullable Activity activity, @NonNull ChatMessageCallback callback) {
        callbacks(api.userData(true).continueWith(api.executorService, (task) -> {
            UserData data = task.getResult();
            if (data == null) throw new IllegalStateException();

            Chat chat = db.getChat(chatId);
            if (chat == null) throw new IllegalStateException();

            Exception lastEx = null;
            for (int i = 0; i < 3; i++) {
                List<OutgoingMessageEnvelope> outgoing = SignalProtocolHelper.encrypt(chat, text);
                JSONArray encryptedMessages = new JSONArray();
                for (OutgoingMessageEnvelope msg : outgoing) encryptedMessages.put(msg.toJson());

                JSONObject body = new JSONObject();
                body.put("chatId", chat.id);
                body.put("messages", encryptedMessages);

                try {
                    api.serverRequest(new Request.Builder()
                            .url(overloadedServerUrl("Chat/Send"))
                            .post(jsonBody(body)));

                    return db.putMessage(chatId, text, System.currentTimeMillis(), data.username);
                } catch (OverloadedApi.OverloadedServerException ex) {
                    lastEx = ex;

                    if (ex.code == 400 && ex.obj != null) {
                        if (!handleMismatchedDevices(chat, ex.obj))
                            throw ex;
                    }
                }
            }

            throw lastEx;
        }), activity, message -> {
            callback.onMessage(message);
            dispatchDecryptedMessage(message);
        }, callback::onFailed);
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


    ///////////////////////////////
    /////// Local messages ////////
    ///////////////////////////////

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


    ///////////////////////////////
    //////// Last message /////////
    ///////////////////////////////

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


    ///////////////////////////////
    /////////// Unread ////////////
    ///////////////////////////////

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


    ///////////////////////////////
    ////// Receive & decrypt //////
    ///////////////////////////////

    /**
     * Sends an acknowledgment to the server (that it has received the message).
     *
     * @param ackId The ack ID
     */
    public void decryptAck(long ackId) {
        loggingCallbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("ackId", ackId);

            api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Ack"))
                    .post(jsonBody(body)));

            return null;
        }), "message-ack: " + ackId);
    }

    private void dispatchDecryptedMessage(@NonNull PlainChatMessage msg) {
        api.dispatchLocalEvent(OverloadedApi.Event.Type.CHAT_MESSAGE, msg);
    }

    @WorkerThread
    void handleEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == OverloadedApi.Event.Type.ENCRYPTED_CHAT_MESSAGE && event.data != null) {
            int chatId = event.data.getInt("chatId");

            PlainChatMessage msg;
            try {
                msg = new EncryptedChatMessage(event.data).decrypt(this, chatId);
            } catch (EncryptedChatMessage.DecryptionException ex) {
                Log.e(TAG, "Failed decrypting message.", ex);
                return;
            }

            dispatchUnreadCountUpdate();
            dispatchDecryptedMessage(msg);
        }
    }

    @Override
    public void close() {
        db.close();
    }

    @UiThread
    public interface UnreadCountListener {
        void mayUpdateUnreadCount();
    }
}
