package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.adapters.Filterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GameInfo {
    public final Game game;
    public final List<Player> players;

    public GameInfo(@NonNull JSONObject obj) throws JSONException {
        game = new Game(obj.getJSONObject("gi"));
        players = Collections.unmodifiableList(Player.list(obj.getJSONArray("pi")));
    }

    /**
     * All the statuses for the player. Order is important and is used to sort the players in the UI.
     */
    public enum PlayerStatus {
        WINNER("sw" /* You have won! */),
        HOST("sh" /* Wait for players then click Start Game. */),
        JUDGE("sj" /* You are the CardcastCard Czar. */),
        JUDGING("sjj" /* Select a winning card. */),
        PLAYING("sp" /* "Select a card to play. */),
        IDLE("si" /* Waiting for players..." */),
        SPECTATOR("sv" /* You are just spectating. */);

        private final String val;

        PlayerStatus(String val) {
            this.val = val;
        }

        @NonNull
        public static PlayerStatus parse(String val) {
            for (PlayerStatus status : values())
                if (Objects.equals(status.val, val))
                    return status;

            throw new IllegalArgumentException("Cannot find purchaseStatus with value: " + val);
        }
    }

    public static class Player implements Filterable<Void> {
        public final String name;
        public final int score;
        public PlayerStatus status;

        public Player(@NonNull JSONObject obj) throws JSONException {
            name = obj.getString("N");
            score = obj.getInt("sc");
            status = PlayerStatus.parse(obj.getString("st"));
        }

        @NonNull
        public static List<Player> list(@NonNull JSONArray array) throws JSONException {
            List<Player> list = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++)
                list.add(new Player(array.getJSONObject(i)));
            return list;
        }

        @Nullable
        @Override
        public Void[] getMatchingFilters() {
            return null;
        }
    }
}
