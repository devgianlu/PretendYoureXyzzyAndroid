package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Request;
import okhttp3.internal.Util;
import xyz.gianlu.pyxoverloaded.callback.ChatCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatMessageCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatMessagesCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatsCallback;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.ChatMessage;
import xyz.gianlu.pyxoverloaded.model.ChatMessages;

import static xyz.gianlu.pyxoverloaded.TaskUtils.callbacks;
import static xyz.gianlu.pyxoverloaded.TaskUtils.loggingCallbacks;
import static xyz.gianlu.pyxoverloaded.Utils.jsonBody;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;
import static xyz.gianlu.pyxoverloaded.Utils.singletonJsonBody;

public class OverloadedChatApi implements Closeable {
    private static final String TAG = OverloadedApi.class.getSimpleName();
    private final OverloadedApi api;
    private final ChatDatabaseHelper db;

    OverloadedChatApi(@NonNull Context context, @NonNull OverloadedApi api) {
        this.api = api;
        this.db = new ChatDatabaseHelper(context);
    }

    public void getSummary() {
        loggingCallbacks(Tasks.call(api.executorService, () -> {
            Long since = db.getLastLastSeen();
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Summary"))
                    .post(singletonJsonBody("since", String.valueOf(since == null ? 0 : since))));

            Iterator<String> iter = obj.keys();
            while (iter.hasNext()) {
                String chatId = iter.next();
                JSONObject chatObj = obj.getJSONObject(chatId);

                Chat chat = db.getChat(chatId);
                if (chat == null) {
                    chat = new Chat(chatObj.getJSONObject("chat"));
                    db.putChat(chat);
                }

                JSONArray array = chatObj.getJSONArray("messages");
                ChatMessages messages = new ChatMessages(array.length(), chat);
                for (int i = 0; i < array.length(); i++)
                    messages.add(new ChatMessage(array.getJSONObject(i)));

                db.putMessages(messages);
                if (!messages.isEmpty()) db.updateLastMessage(chatId, messages.get(0));
            }

            return obj.length();
        }), "chat-summary");
    }

    public void startChat(@NonNull String username, @Nullable Activity activity, @NonNull ChatCallback callback) {
        Chat chat = db.findChatWith(username);
        if (chat != null)
            new Handler(Looper.getMainLooper()).post(() -> callback.onChat(chat));

        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Start"))
                    .post(singletonJsonBody("username", username)));

            Chat remoteChat = new Chat(obj);
            db.putChat(remoteChat);

            JSONObject lastMsgObj = obj.optJSONObject("lastMsg");
            if (lastMsgObj != null)
                db.updateLastMessage(remoteChat.id, new ChatMessage(lastMsgObj));
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

            JSONArray array = obj.getJSONArray("chats");
            List<Chat> chats = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject chatObj = array.getJSONObject(i);
                Chat chat = new Chat(chatObj);
                chats.add(chat);
                db.putChat(chat);

                JSONObject lastMsgObj = chatObj.optJSONObject("lastMsg");
                if (lastMsgObj != null) db.updateLastMessage(chat.id, new ChatMessage(lastMsgObj));
            }

            return chats;
        }), activity, callback::onRemoteChats, callback::onFailed);
    }

    public void sendMessage(@NonNull String chatId, @NonNull String text, @Nullable Activity activity, @NonNull ChatMessageCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("chatId", chatId);
            body.put("message", text);
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Send"))
                    .post(jsonBody(body)));
            return new ChatMessage(obj);
        }), activity, message -> {
            callback.onMessage(message);

            try {
                dispatchMessageSent(chatId, message);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed dispatching message sent.", ex);
            }
        }, callback::onFailed);
    }

    public void getMessages(@NonNull String chatId, int offset, @Nullable Activity activity, @NonNull ChatMessagesCallback callback) {
        ChatMessages list = db.getMessages(chatId, 128, offset);
        if (list != null && !list.isEmpty())
            new Handler(Looper.getMainLooper()).post(() -> callback.onLocalMessages(list));

        callbacks(Tasks.call(api.executorService, () -> {
            Long lastSeen = db.getLastSeen(chatId);
            if (lastSeen == null) lastSeen = 0L;

            JSONObject body = new JSONObject();
            body.put("chatId", chatId);
            body.put("since", lastSeen);
            body.put("limit", 128);
            body.put("offset", offset);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Messages"))
                    .post(jsonBody(body)));

            ChatMessages remoteList = ChatMessages.parse(obj);
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

    private void dispatchMessageSent(@NonNull String chatId, @NonNull ChatMessage msg) throws JSONException {
        JSONObject obj = msg.toJson();
        obj.put("chatId", chatId);
        api.dispatchLocalEvent(OverloadedApi.Event.Type.CHAT_MESSAGE, obj);
    }

    @WorkerThread
    void handleEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == OverloadedApi.Event.Type.CHAT_MESSAGE) {
            String chatId = event.obj.getString("chatId");
            ChatMessage msg = new ChatMessage(event.obj);
            db.addMessage(chatId, msg);
            db.updateLastMessage(chatId, msg);
        }
    }

    public void updateLastSeen(@NonNull String chatId, @NonNull ChatMessage msg) {
        db.updateLastSeen(chatId, msg.timestamp + 1);
    }

    public int countSinceLastSeen(@NonNull String chatId) {
        return db.countSinceLastSeen(chatId);
    }

    @Nullable
    public Long getLastSeen(@NonNull String chatId) {
        return db.getLastSeen(chatId);
    }

    @Nullable
    public ChatMessage getLastMessage(@NonNull String chatId) {
        return db.getLastMessage(chatId);
    }

    @Override
    public void close() {
        db.close();
    }
}
