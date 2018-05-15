package com.gianlu.pretendyourexyzzy.Cards;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.PKeys;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class StarredCardsManager {
    private static List<StarredCard> toStarredCardsList(JSONArray array) throws JSONException {
        List<StarredCard> cards = new ArrayList<>();
        for (int i = 0; i < array.length(); i++)
            cards.add(new StarredCard(array.getJSONObject(i)));
        return cards;
    }

    public static boolean addCard(Context context, StarredCard card) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_CARDS, "[]"));
            List<StarredCard> starredCards = toStarredCardsList(starredCardsArray);

            boolean a = starredCards.contains(card);
            if (!a) starredCards.add(card);
            saveCards(context, starredCards);

            AnalyticsApplication.sendAnalytics(context, Utils.ACTION_STARRED_CARD_ADD);

            return !a;
        } catch (JSONException ex) {
            Logging.log(ex);
            return false;
        }
    }

    private static void saveCards(Context context, List<StarredCard> cards) {
        try {
            JSONArray starredCardsArray = new JSONArray();
            for (StarredCard card : cards) starredCardsArray.put(card.toJson());
            Prefs.putBase64String(context, PKeys.STARRED_CARDS, starredCardsArray.toString());
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    public static void removeCard(Context context, StarredCard card) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_CARDS, "[]"));
            List<StarredCard> starredCards = toStarredCardsList(starredCardsArray);
            starredCards.remove(card);
            saveCards(context, starredCards);
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    @NonNull
    public static List<StarredCard> loadCards(Context context) {
        try {
            JSONArray starredCardsArray = new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_CARDS, "[]"));
            List<StarredCard> cards = toStarredCardsList(starredCardsArray);
            Collections.reverse(cards);
            return cards;
        } catch (JSONException ex) {
            Logging.log(ex);
            return new ArrayList<>();
        }
    }

    public static boolean hasAnyCard(Context context) {
        try {
            return new JSONArray(Prefs.getBase64String(context, PKeys.STARRED_CARDS, "[]")).length() > 0;
        } catch (JSONException ex) {
            Logging.log(ex);
            return false;
        }
    }

    public static class StarredCard implements BaseCard {
        private static final Random random = new Random(); // FIXME
        public final Card blackCard;
        public final CardsGroup whiteCards;
        public final int id;
        private String cachedSentence;

        public StarredCard(Card blackCard, CardsGroup whiteCards) {
            this.blackCard = blackCard;
            this.whiteCards = whiteCards;
            this.id = random.nextInt();
        }

        StarredCard(JSONObject obj) throws JSONException {
            blackCard = new Card(obj.getJSONObject("bc"));
            id = obj.getInt("id");

            whiteCards = new CardsGroup(obj.getJSONArray("wc"));
        }

        @NonNull
        private String createSentence() {
            if (cachedSentence == null) {
                String blackText = blackCard.text;
                if (!blackText.contains("____"))
                    return blackText + "\n<u>" + whiteCards.get(0).text() + "</u>";

                boolean firstCapital = blackText.startsWith("____");
                for (BaseCard whiteCard : whiteCards) {
                    String whiteText = whiteCard.text();
                    if (whiteText.endsWith("."))
                        whiteText = whiteText.substring(0, whiteText.length() - 1);

                    if (firstCapital)
                        whiteText = Character.toUpperCase(whiteText.charAt(0)) + whiteText.substring(1);

                    try {
                        blackText = blackText.replaceFirst("____", "<u>" + whiteText + "</u>");
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        Logging.log(ex);
                    }

                    firstCapital = false;
                }

                cachedSentence = blackText;
            }

            return cachedSentence;
        }

        @NonNull
        @Override
        public String text() {
            return createSentence();
        }

        @Override
        public String watermark() {
            return null;
        }

        @Override
        public int numPick() {
            return -1;
        }

        @Override
        public int numDraw() {
            return -1;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONArray whiteCardsArray = new JSONArray();
            for (BaseCard whiteCard : whiteCards) whiteCardsArray.put(whiteCard.toJson());
            return new JSONObject().put("id", id)
                    .put("wc", whiteCardsArray)
                    .put("bc", blackCard.toJson());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StarredCard that = (StarredCard) o;
            return id == that.id || (blackCard.equals(that.blackCard) && whiteCards.equals(that.whiteCards));
        }

        @Override
        public boolean unknown() {
            return false;
        }

        @Override
        public boolean black() {
            return false;
        }

        @Override
        public boolean writeIn() {
            return false;
        }
    }
}
