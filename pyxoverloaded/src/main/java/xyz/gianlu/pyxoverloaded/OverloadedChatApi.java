package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.logging.Logging;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONException;
import org.json.JSONObject;

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
import static xyz.gianlu.pyxoverloaded.Utils.jsonBody;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;
import static xyz.gianlu.pyxoverloaded.Utils.singletonJsonBody;

public class OverloadedChatApi {
    private final OverloadedApi api;
    private final ChatDatabaseHelper db;

    OverloadedChatApi(@NonNull Context context, @NonNull OverloadedApi api) {
        this.api = api;
        this.db = new ChatDatabaseHelper(context);
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
            if (chat == null) db.putChat(remoteChat);
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

            List<Chat> chats = Chat.parse(obj.getJSONArray("chats"));
            if (!list.isEmpty()) {
                for (Chat chat : chats) {
                    int index = Chat.indexOf(list, chat.id);
                    if (index != -1) {
                        list.get(index).lastMsg = chat.lastMsg;
                        chat.lastSeen = list.get(index).lastSeen;
                    }
                }
            }

            db.putChats(chats);
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
                Logging.log(ex);
            }
        }, callback::onFailed);
    }

    public void getMessages(@NonNull String chatId, int offset, @Nullable Activity activity, @NonNull ChatMessagesCallback callback) {
        final ChatMessages list;
        Chat chat = db.getChat(chatId);
        if (chat != null) {
            list = new ChatMessages(128, chat);
            try (Cursor cursor = db.getMessages(chatId, 128, offset)) {
                if (cursor != null) {
                    while (cursor.moveToNext())
                        list.add(new ChatMessage(cursor));
                }
            }

            if (!list.isEmpty())
                new Handler(Looper.getMainLooper()).post(() -> callback.onLocalMessages(list));
        } else {
            list = null;
        }

        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("chatId", chatId);
            body.put("since", chat == null ? 0 : chat.lastSeen);
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
                list.addAll(remoteList);
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
        }
    }

    public void updateLastSeen(@NonNull String chatId, @NonNull ChatMessage msg) {
        db.updateLastSeen(chatId, msg.timestamp + 1);
    }

    public int countSinceLastSeen(@NonNull String chatId) {
        Chat chat = db.getChat(chatId);
        if (chat == null || chat.lastSeen == 0) return 0;
        else return db.countSinceLastSeen(chatId, chat.lastSeen);
    }

    void close() {
        db.close();
    }
}
