package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.UnknownCard;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class CardsGroup extends ArrayList<BaseCard> {

    private CardsGroup() {
    }

    @NonNull
    public static CardsGroup gameCards(@NonNull JSONArray array) throws JSONException {
        CardsGroup group = new CardsGroup();
        for (int i = 0; i < array.length(); i++) group.add(GameCard.parse(array.getJSONObject(i)));
        return group;
    }

    @NonNull
    public static List<CardsGroup> list(@NonNull JSONArray array) throws JSONException {
        List<CardsGroup> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(gameCards(array.getJSONArray(i)));
        return list;
    }

    @NonNull
    public static CardsGroup singleton(@NonNull BaseCard card) {
        CardsGroup group = new CardsGroup();
        group.add(card);
        return group;
    }

    @NonNull
    public static CardsGroup unknown(int pick) {
        CardsGroup group = new CardsGroup();
        for (int i = 0; i < pick; i++) group.add(new UnknownCard());
        return group;
    }

    @NonNull
    public static CardsGroup from(List<? extends BaseCard> cards) {
        CardsGroup group = new CardsGroup();
        group.addAll(cards);
        return group;
    }

    /**
     * Checks if the group contains a {@link GameCard} with the given ID
     *
     * @param id The card
     * @return Whether the card is contained
     */
    public boolean hasCard(int id) {
        for (BaseCard card : this)
            if (card instanceof GameCard && ((GameCard) card).id == id)
                return true;

        return false;
    }

    /**
     * @return Whether this is a group of unknown cards
     */
    public boolean isUnknwon() {
        return !isEmpty() && get(0) instanceof UnknownCard; // Assuming that if one cards is unknown, also the others are
    }

    @Override
    public final int hashCode() {
        int result = 1;
        for (BaseCard card : this) result = 31 * result + card.hashCode();
        return result;
    }
}