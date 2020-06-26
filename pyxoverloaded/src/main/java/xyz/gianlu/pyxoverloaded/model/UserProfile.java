package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UserProfile {
    public final Long cardsPlayed;
    public final Long roundsPlayed;
    public final Long roundsWon;
    public final Long gamesWon;
    public final List<StarredCard> starredCards;
    public final List<CustomDeck> customDecks;

    public UserProfile(@NonNull JSONObject obj) throws JSONException {
        JSONObject stats = obj.optJSONObject("stats");
        if (stats != null) {
            cardsPlayed = CommonUtils.optLong(stats, "cardsPlayed");
            roundsPlayed = CommonUtils.optLong(stats, "roundsPlayed");
            roundsWon = CommonUtils.optLong(stats, "roundsWon");
            gamesWon = CommonUtils.optLong(stats, "gamesWon");
        } else {
            cardsPlayed = null;
            roundsPlayed = null;
            roundsWon = null;
            gamesWon = null;
        }

        JSONArray starredCardsArray = obj.optJSONArray("starredCards");
        if (starredCardsArray == null) starredCards = new ArrayList<>(0);
        else starredCards = StarredCard.parse(starredCardsArray);

        JSONArray customDecksArray = obj.optJSONArray("customDecks");
        if (customDecksArray == null) customDecks = new ArrayList<>(0);
        else customDecks = CustomDeck.parse(customDecksArray);
    }

    public static class CustomDeck {
        public final String name;
        public final String desc;
        public final String watermark;

        private CustomDeck(@NonNull JSONObject obj) throws JSONException {
            name = obj.getString("name");
            desc = obj.getString("desc");
            watermark = obj.getString("watermark");
        }

        @NonNull
        private static List<CustomDeck> parse(@NonNull JSONArray array) throws JSONException {
            List<CustomDeck> list = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++)
                list.add(new CustomDeck(array.getJSONObject(i)));
            return list;
        }
    }

    public static class CustomDeckWithCards extends CustomDeck {
        public final List<Card> cards;

        public CustomDeckWithCards(@NonNull JSONObject obj) throws JSONException {
            super(obj);
            cards = Card.parse(obj.getJSONArray("cards"));
        }

        @NotNull
        public List<Card> blackCards() {
            LinkedList<Card> list = new LinkedList<>();
            for (Card card : cards)
                if (card.black()) list.add(card);
            return list;
        }


        @NotNull
        public List<Card> whiteCards() {
            LinkedList<Card> list = new LinkedList<>();
            for (Card card : cards)
                if (!card.black()) list.add(card);
            return list;
        }
    }

    public static class StarredCard {
        public final Card blackCard;
        public final Card[] whiteCards;

        private StarredCard(@NonNull JSONObject obj) throws JSONException {
            blackCard = new Card(obj.getJSONObject("bc"));

            JSONArray whiteCardsArray = obj.getJSONArray("wc");
            whiteCards = new Card[whiteCardsArray.length()];
            for (int i = 0; i < whiteCardsArray.length(); i++)
                whiteCards[i] = new Card(whiteCardsArray.getJSONObject(i));
        }

        @NonNull
        private static List<StarredCard> parse(@NonNull JSONArray array) throws JSONException {
            List<StarredCard> list = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++)
                list.add(new StarredCard(array.getJSONObject(i)));
            return list;
        }
    }
}
