package xyz.gianlu.pyxoverloaded.model;

import android.database.Cursor;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.commonutils.adapters.NotFilterable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;

public class Chat implements Filterable<NotFilterable> {
    public final int id;
    public final List<String> participants;
    public final String address;

    public Chat(@NonNull JSONObject obj) throws JSONException {
        id = obj.getInt("id");
        address = obj.getString("address");
        participants = CommonUtils.toStringsList(obj.getJSONArray("participants"), false);
    }

    public Chat(@NonNull Cursor cursor) {
        id = cursor.getInt(cursor.getColumnIndex("id"));
        address = cursor.getString(cursor.getColumnIndex("address"));
        participants = Arrays.asList(cursor.getString(cursor.getColumnIndex("oneParticipant")), cursor.getString(cursor.getColumnIndex("otherParticipant")));
    }

    @NonNull
    public OverloadedUserAddress deviceAddress(int deviceId) {
        return new OverloadedUserAddress(address + ":" + deviceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id == chat.id;
    }

    @Override
    public int hashCode() {
        return id;
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
