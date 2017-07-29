package com.gianlu.pretendyourexyzzy.NetIO.Models;

import com.gianlu.commonutils.Sorting.Filterable;
import com.gianlu.commonutils.Sorting.NotFilterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class Game implements Filterable<NotFilterable>, Serializable {
    public final String host;
    public final Status status;
    public final int gid;
    public final boolean hasPassword;
    public final ArrayList<String> players;
    public final ArrayList<String> spectators;
    public final Options options;

    public Game(JSONObject obj) throws JSONException {
        host = obj.getString("H");
        gid = obj.getInt("gid");
        status = Status.parse(obj.getString("S"));
        hasPassword = obj.getBoolean("hp");
        options = new Options(obj.getJSONObject("go"));

        JSONArray playersArray = obj.getJSONArray("P");
        players = new ArrayList<>();
        for (int i = 0; i < playersArray.length(); i++)
            players.add(playersArray.getString(i));

        JSONArray spectatorsArray = obj.getJSONArray("V");
        spectators = new ArrayList<>();
        for (int i = 0; i < spectatorsArray.length(); i++) {
            String name = spectatorsArray.getString(i);
            if (!spectators.contains(name))
                spectators.add(name);
        }
    }

    @Override
    public NotFilterable getFilterable() {
        return new NotFilterable();
    }

    public enum Status {
        DEALING("d"),
        JUDGING("j"),
        LOBBY("l"),
        PLAYING("p"),
        ROUND_OVER("ro");

        public final String val;

        Status(String val) {
            this.val = val;
        }

        public static Status parse(String val) {
            for (Status status : values())
                if (Objects.equals(status.val, val))
                    return status;

            throw new IllegalArgumentException("Cannot find status with value: " + val);
        }
    }

    public static class NameComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o1.host.compareToIgnoreCase(o2.host);
        }
    }

    public static class NumPlayersComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o2.players.size() - o1.players.size();
        }
    }

    public static class NumSpectatorsComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o2.spectators.size() - o1.spectators.size();
        }
    }

    public class Options implements Serializable {
        public final String timeMultiplier;
        public final int spectatorsLimit;
        public final int playersLimit;
        public final int scoreLimit;
        public final int blanksLimit;
        public final ArrayList<Integer> cardSets;

        public Options(JSONObject obj) throws JSONException {
            timeMultiplier = obj.getString("tm");
            spectatorsLimit = obj.getInt("vL");
            playersLimit = obj.getInt("pL");
            scoreLimit = obj.getInt("sl");
            blanksLimit = obj.getInt("bl");

            JSONArray cardsSetsArray = obj.getJSONArray("css");
            cardSets = new ArrayList<>();
            for (int i = 0; i < cardsSetsArray.length(); i++)
                cardSets.add(cardsSetsArray.getInt(i));
        }
    }
}
