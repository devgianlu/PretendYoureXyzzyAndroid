package com.gianlu.pretendyourexyzzy.Starred;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.PK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class StarredCardsManager {

    public static boolean addCard(@NonNull StarredCard card) {
        try {
            List<StarredCard> cards = StarredCard.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_CARDS, "[]")));

            boolean a = cards.contains(card);
            if (!a) cards.add(card);
            saveCards(cards);

            // TODO: AnalyticsApplication.sendAnalytics( Utils.ACTION_STARRED_CARD_ADD);

            return !a;
        } catch (JSONException ex) {
            Logging.log(ex);
            return false;
        }
    }

    private static void saveCards(List<StarredCard> cards) {
        try {
            JSONArray array = new JSONArray();
            for (StarredCard card : cards) array.put(card.toJson());
            Prefs.putBase64String(PK.STARRED_CARDS, array.toString());
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    public static void removeCard(@NonNull StarredCard card) {
        try {
            List<StarredCard> cards = StarredCard.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_CARDS, "[]")));
            cards.remove(card);
            saveCards(cards);
        } catch (JSONException ex) {
            Logging.log(ex);
        }
    }

    @NonNull
    public static List<StarredCard> loadCards() {
        try {
            List<StarredCard> cards = StarredCard.asList(new JSONArray(Prefs.getBase64String(PK.STARRED_CARDS, "[]")));
            Collections.reverse(cards);
            return cards;
        } catch (JSONException ex) {
            Logging.log(ex);
            return new ArrayList<>();
        }
    }

    public static boolean hasAnyCard() {
        try {
            return new JSONArray(Prefs.getBase64String(PK.STARRED_CARDS, "[]")).length() > 0;
        } catch (JSONException ex) {
            Logging.log(ex);
            return false;
        }
    }

    public static class StarredCard extends BaseCard {
        public final BaseCard blackCard;
        public final CardsGroup whiteCards;
        public final int id;
        private String cachedSentence;

        public StarredCard(@NonNull BaseCard blackCard, @NonNull CardsGroup whiteCards) {
            this.blackCard = blackCard;
            this.whiteCards = whiteCards;
            this.id = ThreadLocalRandom.current().nextInt();
        }

        private StarredCard(JSONObject obj) throws JSONException {
            blackCard = new Card(obj.getJSONObject("bc"));
            id = obj.getInt("id");
            whiteCards = new CardsGroup(obj.getJSONArray("wc"));
        }

        @NonNull
        private static List<StarredCard> asList(JSONArray array) throws JSONException {
            List<StarredCard> cards = new ArrayList<>();
            for (int i = 0; i < array.length(); i++)
                cards.add(new StarredCard(array.getJSONObject(i)));
            return cards;
        }

        @NonNull
        private String createSentence() {
            if (cachedSentence == null) {
                String blackText = blackCard.text();
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
        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONArray array = new JSONArray();
            for (BaseCard whiteCard : whiteCards) array.put(whiteCard.toJson());
            return new JSONObject().put("id", id)
                    .put("wc", array)
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
