package com.gianlu.pretendyourexyzzy.api.crcast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class CrCastDeck {
    public final String deckCode;
    public final String name;
    public final String desc;
    public final State state;
    public final boolean privateDeck;
    public final String lang;
    public final List<CrCastCard> blacks;
    public final List<CrCastCard> whites;
    public final long created;

    CrCastDeck(@NonNull JSONObject obj) throws JSONException, ParseException {
        deckCode = obj.getString("deckcode");
        name = obj.getString("name");
        desc = obj.getString("description");
        lang = obj.getString("decklang");
        state = State.parse(obj.getInt("state"));
        privateDeck = obj.getBoolean("private");
        created = CrCastApi.getApiDateTimeFormat().parse(obj.getString("createdate")).getTime();

        JSONArray blacksArray = obj.getJSONArray("blacks");
        blacks = new ArrayList<>(blacksArray.length());
        for (int i = 0; i < blacksArray.length(); i++)
            blacks.add(new CrCastCard(deckCode, true, blacksArray.getJSONObject(i)));

        JSONArray whitesArray = obj.getJSONArray("whites");
        whites = new ArrayList<>(whitesArray.length());
        for (int i = 0; i < whitesArray.length(); i++)
            whites.add(new CrCastCard(deckCode, false, whitesArray.getJSONObject(i)));
    }

    public enum State {
        ;

        @NonNull
        public static State parse(int state) {
            switch (state) {
                default:
                    throw new IllegalArgumentException("Unknown state: " + state);
            }
        }
    }
}
