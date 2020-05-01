package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

                EncryptedChatMessage.decrypt(OverloadedChatApi.this, chatId, encryptedMessages);
                // TODO: Dispatch messages
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

    @WorkerThread
    private boolean handleMismatchedDevices(@NonNull Chat chat, @NonNull JSONObject obj) throws JSONException, ExecutionException, InterruptedException {
        JSONArray extra = obj.optJSONArray("extra");
        if (extra == null) return false;

        for (int i = 0; i < extra.length(); i++)
            DbSignalStore.get().deleteSession(chat.deviceAddress(extra.getInt(i)).toSignalAddress());

        JSONArray missing = obj.optJSONArray("missing");
        if (missing == null) return false;

        for (int i = 0; i < missing.length(); i++)
            getKeySync(chat.deviceAddress(missing.getInt(i)));

        return true;
    }

    public void sendMessage(int chatId, @NonNull String text, @Nullable Activity activity, @NonNull ChatMessageCallback callback) {
        callbacks(api.userData(true).continueWith(api.executorService, (task) -> {
            UserData data = task.getResult();
            if (data == null) throw new IllegalStateException();

            Chat chat = db.getChat(chatId);
            if (chat == null) throw new IllegalStateException();

            for (int i = 0; i < 3; i++) {
                List<OutgoingMessageEnvelope> outgoing = SignalProtocolHelper.encrypt(chat, text);
                JSONArray encryptedMessages = new JSONArray();
                for (OutgoingMessageEnvelope msg : outgoing) encryptedMessages.put(msg.toJson());

                JSONObject body = new JSONObject();
                body.put("chatId", chat.id);
                body.put("deviceId", SignalProtocolHelper.getLocalDeviceId());
                body.put("messages", encryptedMessages);

                try {
                    api.serverRequest(new Request.Builder()
                            .url(overloadedServerUrl("Chat/Send"))
                            .post(jsonBody(body)));

                    return db.putMessage(chatId, text, System.currentTimeMillis(), data.username);
                } catch (OverloadedApi.OverloadedServerException ex) {
                    if (ex.code == 400 && ex.obj != null) {
                        if (!handleMismatchedDevices(chat, ex.obj))
                            throw ex;
                    }
                }
            }

            throw new IllegalStateException();
        }), activity, message -> {
            callback.onMessage(message);

            try {
                dispatchDecryptedMessage(chatId, message);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed dispatching message sent.", ex);
            }
        }, callback::onFailed);
    }

    public void decryptAck(long ackId) {
        loggingCallbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("ackId", ackId);
            body.put("deviceId", SignalProtocolHelper.getLocalDeviceId());

            api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Ack"))
                    .post(jsonBody(body)));

            return null;
        }), "message-ack: " + ackId);
    }

    @Nullable
    public List<PlainChatMessage> getLocalMessages(int chatId, long since) {
        return db.getMessagesPaginate(chatId, since);
    }

    @NonNull
    public List<PlainChatMessage> getLocalMessages(int chatId) {
        List<PlainChatMessage> msg = db.getMessages(chatId);
        return msg == null ? Collections.emptyList() : msg;
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
        }), "get-keys-" + address.toString()));
    }

    private void dispatchDecryptedMessage(int chatId, @NonNull PlainChatMessage msg) throws JSONException {
        JSONObject obj = msg.toLocalJson();
        obj.put("chatId", chatId);
        api.dispatchLocalEvent(OverloadedApi.Event.Type.CHAT_MESSAGE, obj);
    }

    @WorkerThread
    void handleEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == OverloadedApi.Event.Type.ENCRYPTED_CHAT_MESSAGE) {
            int chatId = event.data.getInt("chatId");

            PlainChatMessage msg;
            try {
                msg = new EncryptedChatMessage(event.data).decrypt(this, chatId);
            } catch (EncryptedChatMessage.DecryptionException ex) {
                Log.e(TAG, "Failed decrypting message.", ex);
                return;
            }

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
