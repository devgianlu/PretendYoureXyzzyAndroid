package com.gianlu.pretendyourexyzzy.Cards;

import com.gianlu.cardcastapi.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;

import org.json.JSONException;
import org.json.JSONObject;

public class FakeCardcastCard implements BaseCard {
    private final Card card;
    private boolean winning;

    public FakeCardcastCard(Card card) {
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
    public boolean isWriteIn() {
        return false;
    }

    @Override
    public int getId() {
        return card.id.hashCode();
    }

    @Override
    public boolean isWinning() {
        return winning;
    }

    @Override
    public void setWinning(boolean winning) {
        this.winning = winning;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        return null; // This isn't needed
    }
}
