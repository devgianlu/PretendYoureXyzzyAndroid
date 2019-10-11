package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class CardsGroup extends ArrayList<BaseCard> {

    public CardsGroup(JSONArray array) throws JSONException {
        for (int j = 0; j < array.length(); j++)
            add(new Card(array.getJSONObject(j)));
    }

    private CardsGroup() {
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
        for (int i = 0; i < pick; i++) group.add(Card.newBlankCard());
        return group;
    }

    @NonNull
    public static List<CardsGroup> list(@NonNull JSONArray array) throws JSONException {
        List<CardsGroup> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(new CardsGroup(array.getJSONArray(i)));
        return list;
    }

    @NonNull
    public static CardsGroup from(List<BaseCard> cards) {
        CardsGroup group = new CardsGroup();
        group.addAll(cards);
        return group;
    }

    public boolean isUnknwon() {
        return !isEmpty() && get(0).unknown(); // Assuming that if one cards is unknown, also the others are
    }

    public boolean hasCard(int id) {
        for (BaseCard card : this)
            if (card.id() == id)
                return true;

        return false;
    }

    public void setWinner() {
        for (BaseCard card : this)
            if (card instanceof Card)
                ((Card) card).setWinner();
    }

    @Override
    public final int hashCode() {
        int result = 1;
        for (BaseCard card : this) result = 31 * result + card.hashCode();
        return result;
    }
}
