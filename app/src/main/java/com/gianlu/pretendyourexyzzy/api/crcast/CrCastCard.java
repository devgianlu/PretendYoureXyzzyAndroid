package com.gianlu.pretendyourexyzzy.api.crcast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import org.json.JSONException;
import org.json.JSONObject;

public final class CrCastCard extends BaseCard {
    public final long id;
    public final CrCastApi.State state;
    private final String deckCode;
    private final boolean black;
    private final String text;
    private final int pick;
    private final int draw;

    CrCastCard(@NonNull String deckCode, boolean black, @NonNull JSONObject obj) throws JSONException {
        this.deckCode = deckCode;
        this.black = black;
        this.id = obj.getLong("id");
        this.text = reformatText(obj.getString("text"), black);
        this.pick = obj.optInt("pick", -1);
        this.draw = obj.optInt("draw", -1);
        this.state = CrCastApi.State.parse(obj.getInt("state"));
    }

    @NonNull
    private static String reformatText(@NonNull String text, boolean black) {
        if (!black) return text;

        if (!text.contains("_")) return text + " ____";
        else return text.replaceAll("_+", "____");
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

    @NonNull
    JSONObject craftJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("text", CommonUtils.toJSONArray(text.split("____", -1)));
        return obj;
    }
}
