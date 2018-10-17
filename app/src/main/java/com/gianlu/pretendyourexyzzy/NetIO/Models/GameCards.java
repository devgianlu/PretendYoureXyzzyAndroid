package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class GameCards {
    public final Card blackCard;
    public final int gameId;
    public final List<CardsGroup> whiteCards;
    public final List<Card> hand;

    public GameCards(JSONObject obj) throws JSONException {
        if (obj.isNull("bc")) blackCard = null;
        else blackCard = new Card(obj.getJSONObject("bc"));
        gameId = obj.getInt("gid");
        whiteCards = Collections.synchronizedList(CardsGroup.list(obj.getJSONArray("wc")));
        hand = Collections.synchronizedList(Card.list(obj.getJSONArray("h")));
    }
}
