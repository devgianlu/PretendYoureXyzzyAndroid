package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class CardSet implements Serializable {
    public final int weight;
    public final int id;
    public final String description;
    public final String name;
    public final int blackCards;
    public final int whiteCards;
    public final boolean baseDeck;
    @Nullable
    public CardcastDeck cardcastDeck;

    @Keep
    public CardSet(JSONObject obj) throws JSONException {
        weight = obj.getInt("w");
        id = obj.getInt("cid");
        description = obj.getString("csd");
        name = obj.getString("csn");
        blackCards = obj.getInt("bcid");
        baseDeck = obj.getBoolean("bd");
        whiteCards = obj.getInt("wcid");
    }
}
