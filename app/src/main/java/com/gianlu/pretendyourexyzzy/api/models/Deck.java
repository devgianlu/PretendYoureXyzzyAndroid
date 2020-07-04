package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    public final int weight;
    public final int id;
    public final String description;
    public final String watermark;
    public final String name;
    public final int blackCards;
    public final int whiteCards;
    public final boolean baseDeck;

    @NonNull
    public static List<Deck> list(JSONArray array) throws JSONException {
        List<Deck> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) list.add(new Deck(array.getJSONObject(i)));
        return list;
    }

    public Deck(JSONObject obj) throws JSONException {
        weight = obj.getInt("w");
        id = obj.getInt("cid");
        description = obj.getString("csd");
        name = obj.getString("csn");
        blackCards = obj.getInt("bcid");
        baseDeck = obj.getBoolean("bd");
        whiteCards = obj.getInt("wcid");
        watermark = CommonUtils.optString(obj, "W");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deck deck = (Deck) o;
        return id == deck.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static int countWhiteCards(List<Deck> decks) {
        int count = 0;
        for (Deck deck : decks) count += deck.whiteCards;
        return count;
    }

    public static int countBlackCards(List<Deck> decks) {
        int count = 0;
        for (Deck deck : decks) count += deck.blackCards;
        return count;
    }
}
