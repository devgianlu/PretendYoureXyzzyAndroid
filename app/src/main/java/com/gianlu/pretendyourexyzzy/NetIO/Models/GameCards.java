package com.gianlu.pretendyourexyzzy.NetIO.Models;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.Cards.CardsGroup;

import org.json.JSONException;
import org.json.JSONObject;

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
        whiteCards = CardsGroup.list(obj.getJSONArray("wc"));
        hand = CommonUtils.toTList(obj.getJSONArray("h"), Card.class);
    }
}
