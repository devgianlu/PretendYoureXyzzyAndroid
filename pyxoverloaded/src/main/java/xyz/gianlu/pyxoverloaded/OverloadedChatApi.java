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
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okhttp3.Request;
import okhttp3.internal.Util;
import xyz.gianlu.pyxoverloaded.callback.ChatCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatMessageCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatMessagesCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatsCallback;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.ChatMessages;
import xyz.gianlu.pyxoverloaded.model.EncryptedChatMessage;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;
import xyz.gianlu.pyxoverloaded.signal.PrefsSessionStore;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

import static xyz.gianlu.pyxoverloaded.TaskUtils.callbacks;
import static xyz.gianlu.pyxoverloaded.TaskUtils.loggingCallbacks;
import static xyz.gianlu.pyxoverloaded.Utils.jsonBody;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;

public class OverloadedChatApi implements Closeable {
    private static final String TAG = OverloadedApi.class.getSimpleName();
    private final OverloadedApi api;
    private final ChatDatabaseHelper db;
    private final List<UnreadCountListener> unreadCountListeners = Collections.synchronizedList(new ArrayList<>());

    OverloadedChatApi(@NonNull Context context, @NonNull OverloadedApi api) {
        this.api = api;
        this.db = new ChatDatabaseHelper(context);
    }

    private static void parseAndStoreDevices(@NonNull JSONArray keys) throws JSONException, InvalidKeyException, UntrustedIdentityException {
        for (int i = 0; i < keys.length(); i++) {
            JSONObject deviceObj = keys.getJSONObject(i);
            SignalProtocolHelper.createSession(new OverloadedUserAddress(deviceObj.getString("address")), Utils.parsePreKeyBundle(deviceObj));
        }
    }

    @Nullable
    public PlainChatMessage getPlainMessage(long msgId) {
        return db.getMessage(msgId);
    }

    @NonNull
    public Task<Integer> getSummary() {
        return loggingCallbacks(Tasks.call(api.executorService, () -> {
            Long since = db.getLastLastSeen();
            JSONObject body = new JSONObject();
            body.put("since", since == null ? 0 : since);
            body.put("signalDeviceId", SignalProtocolHelper.getLocalDeviceId());

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Summary"))
                    .post(jsonBody(body)));

            if (obj.optBoolean("shareKeys", false))
                api.sharePreKeys();

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

                List<PlainChatMessage> decrypted;
                try {
                    decrypted = EncryptedChatMessage.decrypt(OverloadedChatApi.this, encryptedMessages);
                } catch (EncryptedChatMessage.DecryptionException ex) {
                    throw new IllegalStateException(ex);
                }

                ChatMessages messages = new ChatMessages(decrypted, chat);
                db.putMessages(messages);
                if (!messages.isEmpty()) db.updateLastMessage(chatId, messages.get(0));
            }

            return summary.length();
        }), "chat-summary");
    }

    public void startChat(@NonNull String username, @Nullable Activity activity, @NonNull ChatCallback callback) {
        Chat chat = db.findChatWith(username);
        if (chat != null)
            new Handler(Looper.getMainLooper()).post(() -> callback.onChat(chat));

        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("needsKeys", true); // FIXME

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Start"))
                    .post(jsonBody(body)));

            JSONObject chatObj = obj.getJSONObject("chat");
            Chat remoteChat = new Chat(chatObj);
            db.putChat(remoteChat);

            JSONArray keysArray = obj.optJSONArray("keys");
            if (keysArray != null) parseAndStoreDevices(keysArray);

            JSONObject lastMsgObj = chatObj.optJSONObject("lastMsg");
            if (lastMsgObj != null) {
                EncryptedChatMessage ecm = new EncryptedChatMessage(lastMsgObj);

                try {
                    PlainChatMessage msg = ecm.decrypt(OverloadedChatApi.this);
                    db.putMessage(remoteChat.id, msg);
                    db.updateLastMessage(remoteChat.id, msg);
                } catch (EncryptedChatMessage.DecryptionException ex) {
                    Log.e(TAG, "Failed decrypting message.", ex);
                }
            }

            return remoteChat;
        }), activity, remoteChat -> {
            if (chat == null) callback.onChat(remoteChat);
        }, callback::onFailed);
    }

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

                JSONObject lastMsgObj = chatObj.optJSONObject("lastMsg");
                if (lastMsgObj != null) {
                    EncryptedChatMessage ecm = new EncryptedChatMessage(lastMsgObj);

                    try {
                        PlainChatMessage msg = ecm.decrypt(OverloadedChatApi.this);
                        db.putMessage(chat.id, msg);
                        db.updateLastMessage(chat.id, msg);
                    } catch (EncryptedChatMessage.DecryptionException ex) {
                        Log.e(TAG, "Failed decrypting message.", ex);
                    }
                }
            }

            for (Chat chat : local) db.removeChat(chat);

            return chats;
        }), activity, callback::onRemoteChats, callback::onFailed);
    }

    public void sendMessage(int chatId, @NonNull String text, @Nullable Activity activity, @NonNull ChatMessageCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            Chat chat = db.getChat(chatId);
            if (chat == null) throw new IllegalStateException();

            JSONObject body = new JSONObject();
            body.put("chatId", chat.id);
            body.put("sourceDeviceId", SignalProtocolHelper.getLocalDeviceId());

            List<Integer> devices = PrefsSessionStore.get().getSubDeviceSessions(chat.address);
            if (devices.isEmpty()) throw new IllegalStateException();

            JSONArray encryptedMessages = new JSONArray();
            for (Integer deviceId : devices) {
                CiphertextMessage msg = SignalProtocolHelper.encrypt(chat.deviceAddress(deviceId), text);
                JSONObject msgObj = new JSONObject();
                msgObj.put("encrypted", Base64.encodeToString(msg.serialize(), Base64.NO_WRAP));
                msgObj.put("type", msg.getType());
                msgObj.put("destDeviceId", deviceId);
                encryptedMessages.put(msgObj);
            }
            body.put("messages", encryptedMessages);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Send"))
                    .post(jsonBody(body)));

            return new PlainChatMessage(obj.getInt("id"), text, obj.getLong("timestamp"), api.userDataCached().username);
        }), activity, message -> {
            callback.onMessage(message);

            try {
                dispatchDecryptedMessage(chatId, message);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed dispatching message sent.", ex);
            }
        }, callback::onFailed);
    }

    @Nullable
    public List<PlainChatMessage> getLocalMessages(int chatId, long since) {
        return db.getMessagesPaginate(chatId, since);
    }

    public void getMessages(int chatId, @Nullable Activity activity, @NonNull ChatMessagesCallback callback) {
        ChatMessages list = db.getMessages(chatId);
        if (list != null && !list.isEmpty())
            new Handler(Looper.getMainLooper()).post(() -> callback.onLocalMessages(list));

        callbacks(Tasks.call(api.executorService, () -> {
            Long lastSeen = db.getLastSeen(chatId);
            if (lastSeen == null) lastSeen = 0L;

            JSONObject body = new JSONObject();
            body.put("chatId", chatId);
            body.put("since", lastSeen);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Messages"))
                    .post(jsonBody(body)));

            JSONArray messagesArray = obj.getJSONArray("messages");
            List<EncryptedChatMessage> encryptedMessages = new ArrayList<>(messagesArray.length());
            for (int i = 0; i < messagesArray.length(); i++)
                encryptedMessages.add(new EncryptedChatMessage(messagesArray.getJSONObject(i)));

            List<PlainChatMessage> decrypted;
            try {
                decrypted = EncryptedChatMessage.decrypt(OverloadedChatApi.this, encryptedMessages);
            } catch (EncryptedChatMessage.DecryptionException ex) {
                throw new IllegalStateException(ex);
            }

            ChatMessages remoteList = new ChatMessages(decrypted, new Chat(obj));
            db.putMessages(remoteList);
            return remoteList;
        }), activity, remoteList -> {
            if (list != null) {
                list.addAll(0, remoteList); // This are newer, right?
                callback.onRemoteMessages(list);
            } else {
                callback.onRemoteMessages(remoteList);
            }
        }, callback::onFailed);
    }

    public void getKeySync(@NonNull OverloadedUserAddress address) throws ExecutionException, InterruptedException {
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
        }), "get-keys"));
    }

    private void dispatchDecryptedMessage(int chatId, @NonNull PlainChatMessage msg) throws JSONException {
        JSONObject obj = msg.toJson();
        obj.put("chatId", chatId);
        api.dispatchLocalEvent(OverloadedApi.Event.Type.CHAT_MESSAGE, obj);
    }

    @WorkerThread
    void handleEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == OverloadedApi.Event.Type.ENCRYPTED_CHAT_MESSAGE) {
            int chatId = event.data.getInt("chatId");

            PlainChatMessage msg;
            try {
                msg = new EncryptedChatMessage(event.data).decrypt(this);
            } catch (EncryptedChatMessage.DecryptionException ex) {
                Log.e(TAG, "Failed decrypting message.", ex);
                return;
            }

            db.putMessage(chatId, msg);
            db.updateLastMessage(chatId, msg);
            dispatchUnreadCountUpdate();

            dispatchDecryptedMessage(chatId, msg);
        }
    }

    public void updateLastSeen(int chatId, @NonNull PlainChatMessage msg) {
        db.updateLastSeen(chatId, msg.timestamp + 1);
        dispatchUnreadCountUpdate();
    }

    public int countSinceLastSeen(int chatId) {
        return db.countSinceLastSeen(chatId);
    }

    public int countTotalUnread() {
        return db.countTotalUnread();
    }

    @Nullable
    public Long getLastSeen(int chatId) {
        return db.getLastSeen(chatId);
    }

    @Nullable
    public PlainChatMessage getLastMessage(int chatId) {
        return db.getLastMessage(chatId);
    }

    private void dispatchUnreadCountUpdate() {
        synchronized (unreadCountListeners) {
            Handler handler = new Handler(Looper.getMainLooper());
            for (UnreadCountListener listener : unreadCountListeners)
                handler.post(listener::mayUpdateUnreadCount);
        }
    }

    public void addUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.add(listener);
    }

    public void removeUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.remove(listener);
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
