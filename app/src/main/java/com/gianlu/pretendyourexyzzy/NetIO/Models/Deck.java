package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

public class Deck implements Serializable {
    public final int weight;
    public final int id;
    public final String description;
    public final String name;
    public final int blackCards;
    public final int whiteCards;
    public final boolean baseDeck;
    public final String cardcastCode;
    private CardcastDeckInfo cardcastDeck;

    public Deck(JSONObject obj) throws JSONException {
        weight = obj.getInt("w");
        id = obj.getInt("cid");
        description = obj.getString("csd");
        name = obj.getString("csn");
        blackCards = obj.getInt("bcid");
        baseDeck = obj.getBoolean("bd");
        whiteCards = obj.getInt("wcid");
        cardcastCode = getCardcastCode(id);
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

    @Nullable
    private static String getCardcastCode(int id) {
        if (id < 0) {
            String codeTmp = "00000";
            codeTmp += Integer.toString(-id, 36);
            return codeTmp.substring(codeTmp.length() - 5, codeTmp.length());
        }

        return null;
    }

    public void cardcastDeck(CardcastDeckInfo cardcastDeck) {
        this.cardcastDeck = cardcastDeck;
    }

    @Nullable
    public CardcastDeckInfo cardcastDeck() {
        return cardcastDeck;
    }
}
