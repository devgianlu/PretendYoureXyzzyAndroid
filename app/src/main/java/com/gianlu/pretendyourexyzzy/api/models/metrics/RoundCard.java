package com.gianlu.pretendyourexyzzy.api.models.metrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

public class RoundCard extends BaseCard implements Serializable {
    public final String text;
    public final String watermark;
    public final int numPick;
    public final String color;

    RoundCard(JSONObject obj) throws JSONException {
        text = HtmlCompat.fromHtml(obj.getString("Text"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
        watermark = HtmlCompat.fromHtml(obj.getString("Watermark"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString();

        JSONObject meta = obj.getJSONObject("Meta");
        numPick = meta.optInt("Pick", -1);
        color = meta.getString("Color");
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
        return numPick;
    }

    @Override
    public int numDraw() {
        return 0;
    }

    @Override
    public boolean black() {
        return Objects.equals(color, "black");
    }
}
