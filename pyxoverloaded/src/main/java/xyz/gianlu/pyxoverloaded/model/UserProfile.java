package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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
        public final int count;
        public final String shareCode;

        private CustomDeck(@NonNull JSONObject obj) throws JSONException {
            name = obj.getString("name");
            desc = obj.getString("desc");
            watermark = obj.getString("watermark");
            count = obj.getInt("count");
            shareCode = obj.getString("shareCode");
        }

        @Nullable
        public static CustomDeck find(@NonNull List<CustomDeck> decks, @NonNull String name) {
            for (CustomDeck deck : decks)
                if (deck.name.equals(name))
                    return deck;

            return null;
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
        public final String owner;
        public final boolean collaborator;
        private final List<Card> blackCards;
        private final List<Card> whiteCards;

        public CustomDeckWithCards(@NonNull JSONObject obj) throws JSONException {
            super(obj);
            owner = CommonUtils.optString(obj, "owner");
            collaborator = obj.getBoolean("collaborator");
            cards = Card.parse(this, obj.getJSONArray("cards")); // Needs owner

            blackCards = new LinkedList<>();
            whiteCards = new LinkedList<>();
            for (Card card : cards) {
                if (card.black()) blackCards.add(card);
                else whiteCards.add(card);
            }
        }

        @NotNull
        public List<Card> blackCards() {
            return Collections.unmodifiableList(blackCards);
        }

        @NotNull
        public List<Card> whiteCards() {
            return Collections.unmodifiableList(whiteCards);
        }
    }

    public static class StarredCard {
        public final Card blackCard;
        public final Card[] whiteCards;

        private StarredCard(@NonNull JSONObject obj) throws JSONException {
            blackCard = new Card(obj.getJSONObject("bc"), null);

            JSONArray whiteCardsArray = obj.getJSONArray("wc");
            whiteCards = new Card[whiteCardsArray.length()];
            for (int i = 0; i < whiteCardsArray.length(); i++)
                whiteCards[i] = new Card(whiteCardsArray.getJSONObject(i), null);
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
