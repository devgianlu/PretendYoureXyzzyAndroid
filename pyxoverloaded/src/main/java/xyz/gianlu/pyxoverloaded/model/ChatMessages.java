package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatMessages extends ArrayList<ChatMessage> {
    public final Chat chat;

    public ChatMessages(int initialCapacity, @NonNull Chat chat) {
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

    public boolean hasUpdates(@NonNull ChatMessages old) {
        if (old.isEmpty() && isEmpty()) return false;
        else if (old.isEmpty()) return true;
        else return !old.get(old.size() - 1).id.equals(get(size() - 1).id);
    }
}
