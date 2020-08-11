package com.gianlu.pretendyourexyzzy.api.crcast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import org.json.JSONException;
import org.json.JSONObject;

public final class CrCastCard extends BaseCard {
    public final long id;
    public final CrCastDeck.State state;
    private final String deckCode;
    private final boolean black;
    private final String text;
    private final int pick;
    private final int draw;

    CrCastCard(@NonNull String deckCode, boolean black, @NonNull JSONObject obj) throws JSONException {
        this.deckCode = deckCode;
        this.black = black;
        this.id = obj.getLong("id");
        this.text = obj.getString("text");
        this.pick = obj.getInt("pick");
        this.draw = obj.getInt("draw");
        this.state = CrCastDeck.State.parse(obj.getInt("state"));
    }

    @NonNull
    @Override
    public String text() {
        return text;
    }

    @Nullable
    @Override
    public String watermark() {
        return deckCode;
    }

    @Override
    public int numPick() {
        return pick;
    }

    @Override
    public int numDraw() {
        return draw;
    }

    @Override
    public boolean black() {
        return black;
    }
}
