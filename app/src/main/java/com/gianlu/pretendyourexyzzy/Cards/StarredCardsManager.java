package com.gianlu.pretendyourexyzzy.Cards;

import android.content.Context;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.Prefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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

    public static List<? extends BaseCard> loadCards(Context context) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, Prefs.Keys.STARRED_CARDS, "[]"));
            return toStarredCardsList(starredCardsArray);
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
            return new ArrayList<>();
        }
    }

    public static boolean hasAnyCard(Context context) {
        try {
            return new JSONArray(Prefs.getBase64String(context, Prefs.Keys.STARRED_CARDS, "[]")).length() > 0;
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
            return false;
        }
    }

    public static class StarredCard implements BaseCard {
        public final Card blackCard;
        public final List<BaseCard> whiteCards;
        public final int id;

        public StarredCard(Card blackCard, List<? extends BaseCard> whiteCards) {
            this.blackCard = blackCard;
            this.whiteCards = new ArrayList<>();
            this.whiteCards.addAll(whiteCards);
            this.id = new Random().nextInt();
        }

        public StarredCard(JSONObject obj) throws JSONException {
            blackCard = new Card(obj.getJSONObject("bc"));
            id = obj.getInt("id");

            whiteCards = new ArrayList<>();
            JSONArray whiteCardsArray = obj.getJSONArray("wc");
            for (int i = 0; i < whiteCardsArray.length(); i++)
                whiteCards.add(new Card(whiteCardsArray.getJSONObject(i)));
        }

        private String createSentence() {
            String blackText = blackCard.text;
            for (BaseCard whiteCard : whiteCards) {
                try {
                    blackText = blackText.replaceFirst("____", "<u>" + whiteCard.getText() + "</u>");
                } catch (ArrayIndexOutOfBoundsException ex) { // FIXME: Debug
                    System.out.println("BC: " + blackCard.text);
                    throw ex;
                }
            }

            return blackText;
        }

        @Override
        public String getText() {
            return createSentence();
        }

        @Override
        public String getWatermark() {
            return null;
        }

        @Override
        public int getNumPick() {
            return -1;
        }

        @Override
        public int getNumDraw() {
            return -1;
        }

        @Override
        public boolean isWriteIn() {
            return false;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public boolean isWinning() {
            return false;
        }

        @Override
        public void setWinning(boolean winning) {
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONArray whiteCardsArray = new JSONArray();
            for (BaseCard whiteCard : whiteCards) whiteCardsArray.put(whiteCard.toJSON());
            return new JSONObject()
                    .put("id", id)
                    .put("wc", whiteCardsArray)
                    .put("bc", blackCard.toJSON());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredCard that = (StarredCard) o;
            return id == that.id && Objects.equals(blackCard, that.blackCard) && CommonUtils.equals(whiteCards, that.whiteCards);
        }
    }
}
