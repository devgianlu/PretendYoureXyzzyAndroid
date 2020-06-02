package com.gianlu.pretendyourexyzzy.api.models.cards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ContentCard extends BaseCard {
    private final String text;
    private final String watermark;
    private final boolean black;

    public ContentCard(@NonNull String text, @Nullable String watermark, boolean black) {
        this.text = text;
        this.watermark = watermark;
        this.black = black;
    }

    @NonNull
    public static JSONObject toJson(@NonNull GameCard card) throws JSONException {
        return new JSONObject().put("text", card.originalText).put("watermark", card.originalWatermark);
    }

    @NonNull
    public static JSONArray toJson(@NonNull CardsGroup group) throws JSONException {
        JSONArray array = new JSONArray();
        for (BaseCard card : group) {
            if (card instanceof ContentCard) array.put(((ContentCard) card).toJson());
            else if (card instanceof GameCard) array.put(toJson((GameCard) card));
        }
        return array;
    }

    @NonNull
    public static ContentCard parse(@NonNull JSONObject obj, boolean black) throws JSONException {
        return new ContentCard(obj.getString("text"), CommonUtils.optString(obj, "watermark"), black);
    }

    @NonNull
    public static CardsGroup parse(@NonNull JSONArray array, boolean black) throws JSONException {
        List<ContentCard> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) list.add(parse(array.getJSONObject(i), black));
        return CardsGroup.from(list);
    }

    @NonNull
    @Override
    public String text() {
        return text;
    }

    @Nullable
    @Override
    public String watermark() {
        return watermark;
    }

    @Override
    public int numPick() {
        return black ? text.split("____").length - 1 : -1;
    }

    @Override
    public int numDraw() {
        return black ? 0 : -1;
    }

    @Override
    public boolean black() {
        return black;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("text", text).put("watermark", watermark);
    }
}
