package xyz.gianlu.pyxoverloaded.model;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.commonutils.adapters.NotFilterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class Chat implements Filterable<NotFilterable> {
    public final String id;
    public final List<String> participants;

    public Chat(@NonNull JSONObject obj) throws JSONException {
        id = obj.getString("id");
        participants = CommonUtils.toStringsList(obj.getJSONArray("participants"), false);
    }

    public Chat(@NonNull Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndex("id"));
        participants = Arrays.asList(cursor.getString(cursor.getColumnIndex("oneParticipant")), cursor.getString(cursor.getColumnIndex("otherParticipant")));
    }

    @NonNull
    public static List<Chat> parse(@NonNull JSONArray array) throws JSONException {
        List<Chat> chats = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) chats.add(new Chat(array.getJSONObject(i)));
        return chats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id.equals(chat.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Nullable
    public Long lastSeen() {
        return OverloadedApi.chat().getLastSeen(id);
    }

    @Nullable
    public ChatMessage lastMessage() {
        return OverloadedApi.chat().getLastMessage(id);
    }

    @NonNull
    public String getOtherUsername() {
        UserData user = OverloadedApi.get().userDataCached();
        if (user == null) throw new IllegalStateException();
        return participants.get(Objects.equals(participants.get(0), user.username) ? 1 : 0);
    }

    @Override
    public NotFilterable getFilterable() {
        return null;
    }
}
