package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatMessages extends ArrayList<ChatMessage> {
    public final Chat chat;

    private ChatMessages(int initialCapacity, @NonNull Chat chat) {
        super(initialCapacity);
        this.chat = chat;
    }

    @NonNull
    public static ChatMessages parse(@NonNull JSONObject obj) throws JSONException {
        JSONArray array = obj.getJSONArray("messages");
        ChatMessages chats = new ChatMessages(array.length(), new Chat(obj));
        for (int i = 0; i < array.length(); i++)
            chats.add(new ChatMessage(array.getJSONObject(i)));
        return chats;
    }
}
