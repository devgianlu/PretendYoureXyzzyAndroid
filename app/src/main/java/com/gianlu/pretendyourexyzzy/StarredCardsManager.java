package com.gianlu.pretendyourexyzzy;

import android.content.Context;

import com.gianlu.commonutils.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StarredCardsManager {

    public static boolean hasCard(Context context, StarredCard card) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, Prefs.Keys.STARRED_CARDS, "[]"));
            List<StarredCard> starredCards = toStarredCardsList(starredCardsArray);
            return starredCards.contains(card);
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
            return false;
        }
    }

    private static List<StarredCard> toStarredCardsList(JSONArray array) throws JSONException {
        List<StarredCard> cards = new ArrayList<>();
        for (int i = 0; i < array.length(); i++)
            cards.add(new StarredCard(array.getJSONObject(i)));
        return cards;
    }

    public static void addCard(Context context, StarredCard card) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, Prefs.Keys.STARRED_CARDS, "[]"));
            List<StarredCard> starredCards = toStarredCardsList(starredCardsArray);
            if (!starredCards.contains(card)) starredCards.add(card);
            saveCards(context, starredCards);
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
        }
    }

    private static void saveCards(Context context, List<StarredCard> cards) {
        try {
            JSONArray starredCardsArray = new JSONArray();
            for (StarredCard card : cards) starredCardsArray.put(card.toJSON());
            Prefs.putBase64String(context, Prefs.Keys.STARRED_CARDS, starredCardsArray.toString());
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
        }
    }

    public static void removeCard(Context context, StarredCard card) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, Prefs.Keys.STARRED_CARDS, "[]"));
            List<StarredCard> starredCards = toStarredCardsList(starredCardsArray);
            starredCards.remove(card);
            saveCards(context, starredCards);
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
        }
    }

    public static class StarredCard implements Serializable {
        public final int blackCard;
        public final ArrayList<Integer> whiteCards;
        public final String completeSentence;

        public StarredCard(Card blackCard, List<Card> whiteCards) {
            this.blackCard = blackCard.id;
            this.whiteCards = new ArrayList<>();
            for (Card card : whiteCards) this.whiteCards.add(card.id);
            this.completeSentence = createSentence(blackCard, whiteCards);
        }

        public StarredCard(JSONObject obj) throws JSONException {
            blackCard = obj.getInt("bc");

            whiteCards = new ArrayList<>();
            JSONArray whiteCardsArray = obj.getJSONArray("wc");
            for (int i = 0; i < whiteCardsArray.length(); i++)
                whiteCards.add(whiteCardsArray.getInt(i));

            completeSentence = obj.getString("cs");
        }

        private static String createSentence(Card blackCard, List<Card> whiteCards) {
            String blackText = blackCard.text;
            for (Card whiteCard : whiteCards) {
                try {
                    blackText = blackText.replaceFirst("____", "*" + whiteCard.text + "*");
                } catch (ArrayIndexOutOfBoundsException ex) { // FIXME: Debug
                    System.out.println("BC: " + blackCard.text);
                    throw ex;
                }
            }

            return blackText;
        }

        public JSONObject toJSON() throws JSONException {
            JSONArray whiteCardsArray = new JSONArray();
            for (int whiteCard : whiteCards) whiteCardsArray.put(whiteCard);
            return new JSONObject()
                    .put("wc", whiteCardsArray)
                    .put("bc", blackCard)
                    .put("cs", completeSentence);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredCard that = (StarredCard) o;
            return blackCard == that.blackCard && whiteCards.equals(that.whiteCards);
        }
    }
}
