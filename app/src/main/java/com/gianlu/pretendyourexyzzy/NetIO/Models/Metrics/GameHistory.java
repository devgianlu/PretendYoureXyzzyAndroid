package com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GameHistory extends ArrayList<GameHistory.Round> {
    public GameHistory(JSONArray array) throws JSONException {
        super(array.length());
        for (int i = 0; i < array.length(); i++) add(new Round(array.getJSONObject(i)));
    }

    public class Round {
        public final String id;
        public final long timestamp;
        public final RoundCard blackCard;

        Round(JSONObject obj) throws JSONException {
            id = obj.getString("RoundId");
            timestamp = obj.getLong("Timestamp");
            blackCard = new RoundCard(obj.getJSONObject("BlackCard"));
        }
    }
}
