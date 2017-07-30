package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameInfo {
    public final Game game;
    public final List<Player> players;

    public GameInfo(JSONObject obj) throws JSONException {
        game = new Game(obj.getJSONObject("gi"));

        JSONArray playersArray = obj.getJSONArray("pi");
        players = new ArrayList<>();
        for (int i = 0; i < playersArray.length(); i++)
            players.add(new Player(playersArray.getJSONObject(i)));
    }

    public GameInfo(Game game, List<Player> players) {
        this.game = game;
        this.players = players;
    }

    public void notifyPlayerChanged(Player player) {
        int pos = players.indexOf(player);
        if (pos != -1) players.set(pos, player);
    }

    public enum PlayerStatus {
        HOST("sh" /* Wait for players then click Start Game. */),
        IDLE("si" /* Waiting for players..." */),
        JUDGE("sj" /* You are the Card Czar. */),
        JUDGING("sjj" /* Select a winning card. */),
        PLAYING("sp" /* "Select a card to play. */),
        WINNER("sw" /* You have won! */),
        SPECTATOR("sv" /* You are just spectating. */);

        private final String val;

        PlayerStatus(String val) {
            this.val = val;
        }

        public static PlayerStatus parse(String val) {
            for (PlayerStatus status : values())
                if (Objects.equals(status.val, val))
                    return status;

            throw new IllegalArgumentException("Cannot find status with value: " + val);
        }
    }

    public static class Player {
        public final String name;
        public final int score;
        public final PlayerStatus status;

        public Player(JSONObject obj) throws JSONException {
            name = obj.getString("N");
            score = obj.getInt("sc");
            status = PlayerStatus.parse(obj.getString("st"));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Player player = (Player) o;
            return name.equals(player.name);
        }
    }
}
