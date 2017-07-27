package com.gianlu.pretendyourexyzzy.NetIO.Models;

import com.gianlu.commonutils.Sorting.Filterable;
import com.gianlu.commonutils.Sorting.NotFilterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Game implements Filterable<NotFilterable> {
    public final String name;
    public final int gid;
    public final boolean hasPassword;
    public final List<String> players;
    public final List<String> spectators;

    public Game(JSONObject obj) throws JSONException {
        name = obj.getString("H");
        gid = obj.getInt("gid");
        hasPassword = obj.getBoolean("hp");

        JSONArray playersArray = obj.getJSONArray("P");
        players = new ArrayList<>();
        for (int i = 0; i < playersArray.length(); i++)
            players.add(playersArray.getString(i));

        JSONArray spectatorsArray = obj.getJSONArray("V");
        spectators = new ArrayList<>();
        for (int i = 0; i < spectatorsArray.length(); i++)
            spectators.add(spectatorsArray.getString(i));
    }

    @Override
    public NotFilterable getFilterable() {
        return new NotFilterable();
    }

    public static class NameComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o1.name.compareToIgnoreCase(o2.name);
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
}
