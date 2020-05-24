package com.gianlu.pretendyourexyzzy.api.models;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class GameCards {
    public final GameCard blackCard;
    public final int gameId;
    public final List<CardsGroup> whiteCards;
    public final List<BaseCard> hand;

    public GameCards(JSONObject obj) throws JSONException {
        if (obj.isNull("bc")) blackCard = null;
        else blackCard = (GameCard) GameCard.parse(obj.getJSONObject("bc"));
        gameId = obj.getInt("gid");
        whiteCards = Collections.synchronizedList(CardsGroup.list(obj.getJSONArray("wc")));
        hand = Collections.synchronizedList(GameCard.list(obj.getJSONArray("h")));
    }
}
