package xyz.gianlu.pyxoverloaded.model;

import android.database.Cursor;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.commonutils.adapters.NotFilterable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class ChatMessage implements Filterable<NotFilterable> {
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

    public ChatMessage(@NonNull Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndex("id"));
        text = cursor.getString(cursor.getColumnIndex("text"));
        timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
        from = cursor.getString(cursor.getColumnIndex("from"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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

    @Override
    public NotFilterable getFilterable() {
        return new NotFilterable();
    }

    public boolean isFromMe() {
        UserData data = OverloadedApi.get().userDataCached();
        return data != null && Objects.equals(from, data.username);
    }
}
