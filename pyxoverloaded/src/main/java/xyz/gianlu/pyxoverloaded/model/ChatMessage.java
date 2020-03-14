package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {
    public final String id;
    public final String text;
    public final long timestamp;
    public final String from;

    public ChatMessage(@NonNull JSONObject obj) throws JSONException {
        id = obj.getString("id");
        text = obj.getString("text");
        timestamp = obj.getLong("timestamp");
        from = obj.getString("from");
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("text", text);
        obj.put("timestamp", timestamp);
        obj.put("from", from);
        return obj;
    }
}
