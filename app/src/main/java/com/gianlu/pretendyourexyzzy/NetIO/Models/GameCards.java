package com.gianlu.pretendyourexyzzy.NetIO.Models;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.Cards.CardsGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameCards {
    public final Card blackCard;
    public final int gameId;
    public final List<CardsGroup<Card>> whiteCards;
    public final List<Card> hand;

    public GameCards(JSONObject obj) throws JSONException {
        if (obj.isNull("bc")) blackCard = null;
        else blackCard = new Card(obj.getJSONObject("bc"));
        gameId = obj.getInt("gid");
        whiteCards = toWhiteCardsList(obj.getJSONArray("wc"));
        hand = CommonUtils.toTList(obj.getJSONArray("h"), Card.class);
    }

    public static List<CardsGroup<Card>> toWhiteCardsList(JSONArray whiteCardsArray) throws JSONException {
        List<CardsGroup<Card>> whiteCards = new ArrayList<>();
        for (int i = 0; i < whiteCardsArray.length(); i++) {
            CardsGroup<Card> group = new CardsGroup<>();
            JSONArray whiteCardsSubArray = whiteCardsArray.getJSONArray(i);
            for (int j = 0; j < whiteCardsSubArray.length(); j++)
                group.add(new Card(whiteCardsSubArray.getJSONObject(j)));

            whiteCards.add(group);
        }

        return whiteCards;
    }
}
