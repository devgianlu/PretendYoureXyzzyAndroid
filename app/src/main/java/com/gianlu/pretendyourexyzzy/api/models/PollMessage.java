package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.logging.Logging;

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
    public final JSONObject obj;
    public final Event event;
    public final boolean emote;
    public final boolean wall;

    private PollMessage(JSONObject obj) throws JSONException {
        event = Event.parse(obj.getString("E"));
        sender = CommonUtils.optString(obj, "f");
        message = CommonUtils.optString(obj, "m");
        gid = obj.optInt("gid", -1);
        timestamp = obj.optLong("ts", -1);
        emote = obj.optBoolean("me", false);
        wall = obj.optBoolean("wall", false);

        this.obj = obj;
    }

    @NonNull
    public static List<PollMessage> list(JSONArray array) throws JSONException {
        List<PollMessage> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(new PollMessage(array.getJSONObject(i)));
            } catch (IllegalArgumentException ex) {
                Logging.log("Skipping poll message due to illegal event.", ex);
            }
        }

        return list;
    }

    public enum Event {
        BANNED("B&"),
        CARDCAST_ADD_CARDSET("cac"),
        CARDCAST_REMOVE_CARDSET("crc"),
        CHAT("c"),
        GAME_BLACK_RESHUFFLE("gbr"),
        GAME_JUDGE_LEFT("gjl"),
        GAME_JUDGE_SKIPPED("gjs"),
        GAME_LIST_REFRESH("glr"),
        GAME_OPTIONS_CHANGED("goc"),
        GAME_PLAYER_INFO_CHANGE("gpic"),
        GAME_PLAYER_JOIN("gpj"),
        GAME_PLAYER_KICKED_IDLE("gpki"),
        GAME_PLAYER_LEAVE("gpl"),
        GAME_PLAYER_SKIPPED("gps"),
        GAME_SPECTATOR_JOIN("gvj"),
        GAME_SPECTATOR_LEAVE("gvl"),
        GAME_ROUND_COMPLETE("grc"),
        GAME_STATE_CHANGE("gsc"),
        GAME_WHITE_RESHUFFLE("gwr"),
        HAND_DEAL("hd"),
        HURRY_UP("hu"),
        KICKED("k"),
        KICKED_FROM_GAME_IDLE("kfgi"),
        NEW_PLAYER("np"),
        NOOP("_"),
        PLAYER_LEAVE("pl");

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
