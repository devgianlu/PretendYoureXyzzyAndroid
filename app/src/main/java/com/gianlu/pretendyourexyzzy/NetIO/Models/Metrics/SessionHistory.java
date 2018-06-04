package com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionHistory {
    public final long loginTimestamp;
    public final String pid;
    public final List<Game> games;
    public final List<SimpleRound> playedRounds;
    public final List<SimpleRound> judgedRounds;

    public SessionHistory(JSONObject obj) throws JSONException {
        pid = obj.getString("PersistentId");
        loginTimestamp = obj.getLong("LogInTimestamp");

        JSONArray gamesArray = obj.getJSONArray("Games");
        games = new ArrayList<>(gamesArray.length());
        for (int i = 0; i < gamesArray.length(); i++)
            games.add(new Game(gamesArray.getJSONObject(i)));

        JSONArray playedArray = obj.getJSONArray("PlayedRounds");
        playedRounds = new ArrayList<>(playedArray.length());
        for (int i = 0; i < playedArray.length(); i++)
            playedRounds.add(new SimpleRound(playedArray.getJSONObject(i)));

        JSONArray judgedArray = obj.getJSONArray("JudgedRounds");
        judgedRounds = new ArrayList<>(judgedArray.length());
        for (int i = 0; i < judgedArray.length(); i++)
            judgedRounds.add(new SimpleRound(judgedArray.getJSONObject(i)));
    }

    public class Game {
        public final String id;
        public final long timestamp;

        public Game(JSONObject obj) throws JSONException {
            id = obj.getString("GameId");
            timestamp = obj.getLong("Timestamp");
        }
    }
}
