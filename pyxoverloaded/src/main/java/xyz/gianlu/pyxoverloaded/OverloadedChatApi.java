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
            db.putChat(remoteChat);
            if (chat != null && chat.lastMsg == null) chat.lastMsg = remoteChat.lastMsg;
            return remoteChat;
        }), activity, remoteChat -> {
            if (chat == null) callback.onChat(remoteChat);
        }, callback::onFailed);
    }

    public void listChats(@Nullable Activity activity, @NonNull ChatsCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/List"))
                    .post(Util.EMPTY_REQUEST));

            List<Chat> chats = Chat.parse(obj.getJSONArray("chats"));
            db.putChats(chats);
            return chats;
        }), activity, callback::onChats, callback::onFailed);
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

    public void getMessages(@NonNull String chatId, @Nullable Activity activity, @NonNull ChatMessagesCallback callback) {
        final ChatMessages list;
        Chat chat = db.getChat(chatId);
        if (chat != null) {
            list = new ChatMessages(128, chat);
            try (Cursor cursor = db.getMessages(chatId, 128)) {
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

            // TODO: Not implemented on server
            body.put("since", chat == null ? 0 : chat.lastSeen);
            body.put("limit", 128);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Messages"))
                    .post(jsonBody(body)));

            ChatMessages removeList = ChatMessages.parse(obj);
            db.putMessages(removeList);
            return removeList;
        }), activity, remoteList -> {
            if (list == null || remoteList.hasUpdates(list))
                callback.onRemoteMessages(remoteList);
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
}
