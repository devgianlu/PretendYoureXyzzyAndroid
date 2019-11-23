package com.gianlu.pretendyourexyzzy.api.models.metrics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class SimpleRound implements Serializable {
    public final String id;
    public final long timestamp;
    public final RoundCard blackCard;

    SimpleRound(JSONObject obj) throws JSONException {
        id = obj.getString("RoundId");
        timestamp = obj.getLong("Timestamp");
        blackCard = new RoundCard(obj.getJSONObject("BlackCard"));
    }
}
