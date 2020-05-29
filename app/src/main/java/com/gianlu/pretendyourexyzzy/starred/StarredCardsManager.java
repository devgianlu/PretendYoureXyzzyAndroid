package com.gianlu.pretendyourexyzzy.starred;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class StarredCardsManager {
    private static final String TAG = StarredCardsManager.class.getSimpleName();
    private static StarredCardsManager instance;
    private final JsonStoring storing;
    private final List<StarredCard> list;

    private StarredCardsManager() {
        storing = JsonStoring.intoPrefs();
        list = new ArrayList<>();
        loadCards();
    }

    @NonNull
    public static StarredCardsManager get() {
        if (instance == null) instance = new StarredCardsManager();
        return instance;
    }

    public boolean addCard(@NonNull StarredCard card) {
        boolean a = list.contains(card);
        if (!a) list.add(card);
        saveCards();
        return !a;
    }

    public void removeCard(@NonNull StarredCard card) {
        list.remove(card);
        saveCards();
    }

    @NonNull
    public List<StarredCard> getCards() {
        return list;
    }

    private void saveCards() {
        try {
            JSONArray array = new JSONArray();
            for (StarredCard card : list) array.put(card.toJson());
            storing.putJsonArray(PK.STARRED_CARDS, array);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed saving JSON.", ex);
        }
    }

    private void loadCards() {
        try {
            list.clear();
            list.addAll(StarredCard.asList(storing.getJsonArray(PK.STARRED_CARDS)));
            Collections.sort(list, new AddedAtComparator());
        } catch (JSONException ex) {
            Log.e(TAG, "Failed loading JSON.", ex);
        }
    }

    public boolean hasAnyCard() {
        return !list.isEmpty();
    }

    private static class AddedAtComparator implements Comparator<StarredCard> {

        @Override
        public int compare(StarredCard o1, StarredCard o2) {
            if (o1.addedAt == o2.addedAt) return 0;
            else return o1.addedAt > o2.addedAt ? -1 : 1;
        }
    }

    public static class StarredCard extends BaseCard {
        public final GameCard blackCard;
        public final CardsGroup whiteCards;
        public final int id;
        private final long addedAt;
        private String cachedSentence;

        public StarredCard(@NonNull GameCard blackCard, @NonNull CardsGroup whiteCards) {
            this.blackCard = blackCard;
            this.whiteCards = whiteCards;
            this.id = ThreadLocalRandom.current().nextInt();
            this.addedAt = System.currentTimeMillis();
        }

        private StarredCard(JSONObject obj) throws JSONException {
            blackCard = (GameCard) GameCard.parse(obj.getJSONObject("bc"));
            id = obj.getInt("id");
            whiteCards = CardsGroup.gameCards(obj.getJSONArray("wc"));
            addedAt = obj.optLong("addedAt", System.currentTimeMillis());
        }

        @NonNull
        private static List<StarredCard> asList(@Nullable JSONArray array) throws JSONException {
            if (array == null) return new ArrayList<>();

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
                    } catch (ArrayIndexOutOfBoundsException ignored) {
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

        @NonNull
        JSONObject toJson() throws JSONException {
            JSONArray array = new JSONArray();
            for (BaseCard whiteCard : whiteCards) {
                if (whiteCard instanceof GameCard)
                    array.put(((GameCard) whiteCard).toJson());
            }

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
        public boolean black() {
            return false;
        }
    }
}
