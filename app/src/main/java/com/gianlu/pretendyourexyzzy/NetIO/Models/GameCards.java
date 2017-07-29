package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameCards {
    public final Card blackCard;
    public final int gameId;
    public final List<List<Card>> whiteCards;
    public final List<Card> hand;

    public GameCards(JSONObject obj) throws JSONException {
        if (obj.isNull("bc")) blackCard = null;
        else blackCard = new Card(obj.getJSONObject("bc"));
        gameId = obj.getInt("gid");
        whiteCards = toWhiteCardsList(obj.getJSONArray("wc"));

        hand = new ArrayList<>();
        JSONArray handArray = obj.getJSONArray("h");
        for (int i = 0; i < handArray.length(); i++)
            hand.add(new Card(handArray.getJSONObject(i)));
    }

    public static List<List<Card>> toWhiteCardsList(JSONArray whiteCardsArray) throws JSONException {
        List<List<Card>> whiteCards = new ArrayList<>();
        for (int i = 0; i < whiteCardsArray.length(); i++) {
            List<Card> whiteCardsSub = new ArrayList<>();
            JSONArray whiteCardsSubArray = whiteCardsArray.getJSONArray(i);
            for (int j = 0; j < whiteCardsSubArray.length(); j++)
                whiteCardsSub.add(new Card(whiteCardsSubArray.getJSONObject(j)));

            whiteCards.add(whiteCardsSub);
        }

        return whiteCards;
    }
}
