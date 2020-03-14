package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class Chat {
    public final String id;
    public final List<String> participants;
    public ChatMessage lastMsg;

    public Chat(@NonNull JSONObject obj) throws JSONException {
        id = obj.getString("id");
        participants = CommonUtils.toStringsList(obj.getJSONArray("participants"), false);
        JSONObject lastMsgObj = obj.optJSONObject("lastMsg");
        if (lastMsgObj != null) lastMsg = new ChatMessage(lastMsgObj);
        else lastMsg = null;
    }

    @NonNull
    public static List<Chat> parse(@NonNull JSONArray array) throws JSONException {
        List<Chat> chats = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) chats.add(new Chat(array.getJSONObject(i)));
        return chats;
    }

    @NonNull
    public String getOtherUsername() {
        UserData user = OverloadedApi.get().userDataCached();
        if (user == null) throw new IllegalStateException();
        return participants.get(Objects.equals(participants.get(0), user.username) ? 1 : 0);
    }
}
