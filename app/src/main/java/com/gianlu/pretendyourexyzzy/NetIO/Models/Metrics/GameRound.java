package com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameRound {
    public final String gameId;
    public final long timestamp;
    public final RoundCard blackCard;
    public final List<RoundCard> winningCard;
    public final List<List<RoundCard>> otherCards;

    public GameRound(JSONObject obj) throws JSONException {
        gameId = obj.getString("GameId");
        timestamp = obj.getLong("Timestamp");
        blackCard = new RoundCard(obj.getJSONObject("BlackCard"));

        JSONArray winningPlay = obj.getJSONArray("WinningPlay");
        winningCard = new ArrayList<>(winningPlay.length());
        for (int i = 0; i < winningPlay.length(); i++)
            winningCard.add(new RoundCard(winningPlay.getJSONObject(i)));

        JSONArray otherPlays = obj.getJSONArray("OtherPlays");
        otherCards = new ArrayList<>(otherPlays.length());
        for (int i = 0; i < otherPlays.length(); i++) {
            JSONArray sub = otherPlays.getJSONArray(i);
            List<RoundCard> subCards = new ArrayList<>(sub.length());
            otherCards.add(subCards);
            for (int j = 0; j < sub.length(); j++)
                subCards.add(new RoundCard(sub.getJSONObject(j)));
        }
    }

    public int whiteCards() {
        int count = winningCard.size();
        for (List<RoundCard> cards : otherCards) count += cards.size();
        return count;
    }
}
