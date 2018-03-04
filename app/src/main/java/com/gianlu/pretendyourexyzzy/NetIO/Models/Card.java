package com.gianlu.pretendyourexyzzy.NetIO.Models;


import android.support.annotation.Keep;

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
    public boolean isUnknown() {
        return id == -1 && watermark.isEmpty() && originalText.isEmpty();
    }

    @Override
    public boolean isBlack() {
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

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getWatermark() {
        return watermark;
    }

    @Override
    public int getNumPick() {
        return numPick;
    }

    @Override
    public int getNumDraw() {
        return numDraw;
    }

    public boolean isWriteIn() {
        return writeIn;
    }

    @Override
    public int getId() {
        return id;
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }
}
