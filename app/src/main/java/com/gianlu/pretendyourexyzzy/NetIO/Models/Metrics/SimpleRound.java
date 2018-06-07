package com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics;

import org.json.JSONException;
import org.json.JSONObject;

public class SimpleRound {
    public final String id;
    public final long timestamp;
    public final RoundCard blackCard;

    SimpleRound(JSONObject obj) throws JSONException {
        id = obj.getString("RoundId");
        timestamp = obj.getLong("Timestamp");
        blackCard = new RoundCard(obj.getJSONObject("BlackCard"));
    }
}
