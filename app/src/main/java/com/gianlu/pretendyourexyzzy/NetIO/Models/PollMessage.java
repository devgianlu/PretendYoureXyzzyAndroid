package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PollMessage {
    public final String sender;
    public final String message;
    public final int gid;
    public final long timestamp;
    public final Event event;

    public PollMessage(JSONObject obj) throws JSONException {
        event = Event.parse(obj.getString("E"));
        sender = obj.optString("f", null);
        message = obj.optString("m", null);
        gid = obj.optInt("gid", -1);
        timestamp = obj.getLong("ts");
    }

    public static List<PollMessage> toPollMessagesList(JSONArray array) throws JSONException {
        List<PollMessage> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(new PollMessage(array.getJSONObject(i)));
        return list;
    }

    public enum Event {
        CHAT("c"),
        NOOP("_"),
        GAME_PLAYER_JOIN("gpj"),
        GAME_LIST_REFRESH("glr");

        private final String val;

        Event(String val) {
            this.val = val;
        }

        public static Event parse(String val) {
            for (Event event : values())
                if (Objects.equals(event.val, val))
                    return event;

            throw new IllegalArgumentException("Cannot find an event for value: " + val);
        }
    }
}
