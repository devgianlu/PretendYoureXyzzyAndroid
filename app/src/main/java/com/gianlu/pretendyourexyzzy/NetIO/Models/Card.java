package com.gianlu.pretendyourexyzzy.NetIO.Models;


import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Card implements BaseCard {
    private static final Pattern CARD_NUM_PATTERN = Pattern.compile("<span class=\"cardnum\">(\\d*)\\s?(?:\\\\|/)\\s?(\\d*)</span>");
    public final int id;
    public final String text;
    public final String watermark;
    public final int numPick;
    public final int numDraw;
    public final boolean writeIn;
    private final String originalText;
    private final String originalWatermark;
    public boolean winner = false;

    @Keep
    public Card(JSONObject obj) throws JSONException {
        id = obj.getInt("cid");

        numPick = obj.optInt("PK", -1);
        numDraw = obj.optInt("D", -1);
        writeIn = obj.optBoolean("wi", false);

        String textTmp = originalText = obj.getString("T").replace(" - ", "\n");
        String watermarkTmp = originalWatermark = obj.getString("W");
        Matcher matcher = CARD_NUM_PATTERN.matcher(textTmp);
        if (matcher.find()) {
            text = textTmp.replace(matcher.group(), "").trim();
            watermark = watermarkTmp + " (" + matcher.group(1) + "/" + matcher.group(2) + ")";
        } else {
            watermark = watermarkTmp;
            text = textTmp.trim();
        }
    }

    private Card(int id, String text, String watermark, int numPick, int numDraw, boolean writeIn) {
        this.id = id;
        this.text = this.originalText = text;
        this.watermark = this.originalWatermark = watermark;
        this.numPick = numPick;
        this.numDraw = numDraw;
        this.writeIn = writeIn;
    }

    @NonNull
    public static Card newBlankCard() {
        return new Card(-1, "", "", -1, -1, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return id == card.id;
    }

    @Override
    public boolean unknown() {
        return id == -1 && watermark.isEmpty() && originalText.isEmpty();
    }

    @Override
    public boolean black() {
        return numPick != -1;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("cid", id)
                .put("PK", numPick)
                .put("D", numDraw)
                .put("wi", writeIn)
                .put("T", originalText)
                .put("W", originalWatermark);
    }

    @NonNull
    @Override
    public String text() {
        return text;
    }

    @Override
    @Nullable
    public String watermark() {
        return watermark;
    }

    @Override
    public int numPick() {
        return numPick;
    }

    @Override
    public int numDraw() {
        return numDraw;
    }

    public boolean writeIn() {
        return writeIn;
    }

    @Override
    public int id() {
        return id;
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner() {
        this.winner = true;
    }
}
