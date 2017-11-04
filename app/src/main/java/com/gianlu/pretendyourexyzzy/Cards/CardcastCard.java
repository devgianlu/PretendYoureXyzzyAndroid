package com.gianlu.pretendyourexyzzy.Cards;

import com.gianlu.cardcastapi.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;

public class CardcastCard implements BaseCard {
    private final Card card;

    public CardcastCard(Card card) {
        this.card = card;
    }

    @Override
    public String getText() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (String split : card.text) {
            if (!first) builder.append(" ____ ");
            first = false;
            builder.append(split);
        }

        return builder.toString();
    }

    @Override
    public String getWatermark() {
        return card.deckCode;
    }

    @Override
    public int getNumPick() {
        if (card.text.size() == 1) return -1;
        return card.text.size() - 1;
    }

    @Override
    public int getNumDraw() {
        return 0;
    }

    @Override
    public int getId() {
        return card.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardcastCard that = (CardcastCard) o;
        return card.equals(that.card);
    }

    @Override
    public boolean isUnknown() {
        return false;
    }
}
