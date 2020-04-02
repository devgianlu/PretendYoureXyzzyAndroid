package xyz.gianlu.pyxoverloaded.model;

import android.database.Cursor;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.commonutils.adapters.NotFilterable;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class PlainChatMessage implements Filterable<NotFilterable> {
    public final long id;
    public final String text;
    public final long timestamp;
    public final String from;

    private PlainChatMessage(@NonNull JSONObject obj) throws JSONException {
        id = obj.getLong("id");
        text = obj.getString("text");
        timestamp = obj.getLong("timestamp");
        from = obj.getString("from");
    }

    public PlainChatMessage(@NonNull Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex("id"));
        text = cursor.getString(cursor.getColumnIndex("text"));
        timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
        from = cursor.getString(cursor.getColumnIndex("from"));
    }

    public PlainChatMessage(long id, String text, long timestamp, String from) {
        this.id = id;
        this.text = text;
        this.timestamp = timestamp;
        this.from = from;
    }

    @Contract("_ -> new")
    @NonNull
    public static PlainChatMessage fromLocal(@NonNull JSONObject obj) throws JSONException {
        return new PlainChatMessage(obj);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlainChatMessage that = (PlainChatMessage) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
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
