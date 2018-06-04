package com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class GameHistory extends ArrayList<SimpleRound> {
    public GameHistory(JSONArray array) throws JSONException {
        super(array.length());
        for (int i = 0; i < array.length(); i++) add(new SimpleRound(array.getJSONObject(i)));
    }
}
