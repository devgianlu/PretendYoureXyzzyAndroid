package xyz.gianlu.pyxoverloaded.model;

import android.database.Cursor;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.commonutils.adapters.NotFilterable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class PlainChatMessage implements Filterable<NotFilterable> {
    public final int chatId;
    public final long rowId;
    public final String text;
    public final long timestamp;
    public final String from;

    public PlainChatMessage(int chatId, @NonNull Cursor cursor) {
        this.chatId = chatId;
        rowId = cursor.getLong(cursor.getColumnIndex("rowid"));
        text = cursor.getString(cursor.getColumnIndex("text"));
        timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
        from = cursor.getString(cursor.getColumnIndex("from"));
    }

    public PlainChatMessage(int chatId, long rowId, String text, long timestamp, String from) {
        this.chatId = chatId;
        this.rowId = rowId;
        this.text = text;
        this.timestamp = timestamp;
        this.from = from;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlainChatMessage that = (PlainChatMessage) o;
        return rowId == that.rowId;
    }

    @Override
    public int hashCode() {
        return (int) (rowId ^ (rowId >>> 32));
    }

    @NonNull
    public JSONObject toLocalJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("rowId", rowId);
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
