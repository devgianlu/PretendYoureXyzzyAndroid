package com.gianlu.pretendyourexyzzy.Cards;

import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import java.util.ArrayList;

public class CardsGroup<C extends BaseCard> extends ArrayList<C> {

    public static <C extends BaseCard> CardsGroup<C> singleton(C card) {
        CardsGroup<C> group = new CardsGroup<>();
        group.add(card);
        return group;
    }

    public boolean hasCard(int id) {
        for (BaseCard card : this)
            if (card.getId() == id)
                return true;

        return false;
    }

    public void setWinner() {
        for (BaseCard card : this)
            if (card instanceof Card)
                ((Card) card).setWinner();
    }
}
