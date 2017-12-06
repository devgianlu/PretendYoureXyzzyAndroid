package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

public class CardcastDecks extends ArrayList<CardcastDeck> {
    public final int totalDecks;
    public final int offset;
    public final int count;

    public CardcastDecks(JSONObject obj) throws JSONException, ParseException {
        totalDecks = obj.getInt("total");

        JSONObject results = obj.getJSONObject("results");
        count = results.getInt("count");
        offset = results.getInt("offset");

        JSONArray decksArray = results.getJSONArray("data");
        for (int i = 0; i < decksArray.length(); i++)
            add(new CardcastDeck(decksArray.getJSONObject(i)));
    }

    private CardcastDecks() {
        totalDecks = 1;
        offset = 0;
        count = 1;
    }

    public static CardcastDecks singleton(CardcastDeckInfo info) {
        CardcastDecks decks = new CardcastDecks();
        decks.add(info);
        return decks;
    }
}
