package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.logging.Logging;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONException;
import org.json.JSONObject;

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
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Start"))
                    .post(singletonJsonBody("username", username)));
            return new Chat(obj);
        }), activity, callback::onChat, callback::onFailed);
    }

    public void listChats(@Nullable Activity activity, @NonNull ChatsCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/List"))
                    .post(Util.EMPTY_REQUEST));
            return Chat.parse(obj.getJSONArray("chats"));
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
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("chatId", chatId);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Messages"))
                    .post(jsonBody(body)));
            return ChatMessages.parse(obj);
        }), activity, callback::onMessages, callback::onFailed);
    }

    private void dispatchMessageSent(@NonNull String chatId, @NonNull ChatMessage msg) throws JSONException {
        JSONObject obj = msg.toJson();
        obj.put("chatId", chatId);
        api.dispatchLocalEvent(OverloadedApi.Event.Type.CHAT_MESSAGE, obj);
    }

    @WorkerThread
    void handleEvent(@NonNull OverloadedApi.Event event) {
        if (event.type == OverloadedApi.Event.Type.CHAT_MESSAGE) {

        }
    }
}
