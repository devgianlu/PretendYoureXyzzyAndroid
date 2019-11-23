package com.gianlu.pretendyourexyzzy.api.models.metrics;

import org.json.JSONException;
import org.json.JSONObject;

public class SessionStats {
    public final String id;
    public final int playedRounds;
    public final int judgedRounds;

    public SessionStats(JSONObject obj) throws JSONException {
        id = obj.getString("SessionId");
        playedRounds = obj.getInt("PlayedRoundCount");
        judgedRounds = obj.getInt("JudgedRoundCount");
    }
}
