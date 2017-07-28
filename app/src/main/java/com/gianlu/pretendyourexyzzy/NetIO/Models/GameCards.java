package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// TODO: What's h??
public class GameCards {
    public final Card blackCard;
    public final int gameId;
    public final List<List<Card>> whiteCards;

    public GameCards(JSONObject obj) throws JSONException {
        blackCard = new Card(obj.getJSONObject("bc"));
        gameId = obj.getInt("gid");

        whiteCards = new ArrayList<>();
        JSONArray whiteCardsArray = obj.getJSONArray("wc");
        for (int i = 0; i < whiteCardsArray.length(); i++) {
            List<Card> whiteCardsSub = new ArrayList<>();
            JSONArray whiteCardsSubArray = whiteCardsArray.getJSONArray(i);
            for (int j = 0; j < whiteCardsSubArray.length(); j++)
                whiteCardsSub.add(new Card(whiteCardsSubArray.getJSONObject(j)));

            whiteCards.add(whiteCardsSub);
        }
    }
}
