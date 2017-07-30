package com.gianlu.pretendyourexyzzy.NetIO.Models;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Card {
    private static final Pattern CARD_NUM_PATTERN = Pattern.compile("<span class=\"cardnum\">(\\d+) / (\\d+)</span>");
    public final int id;
    public final String text;
    public final String watermark;
    public final int numPick;
    public final int numDraw;
    public final boolean writeIn;
    public boolean winning = false;

    public Card(JSONObject obj) throws JSONException {
        id = obj.getInt("cid");

        numPick = obj.optInt("PK", -1);
        numDraw = obj.optInt("D", -1);
        writeIn = obj.optBoolean("wi", false);

        String textTmp = obj.getString("T");
        String watermarkTmp = obj.getString("W");
        Matcher matcher = CARD_NUM_PATTERN.matcher(textTmp);
        if (matcher.find()) {
            text = textTmp.replace(matcher.group(), "").trim();
            watermark = watermarkTmp + " (" + matcher.group(1) + "/" + matcher.group(2) + ")";
        } else {
            watermark = watermarkTmp;
            text = textTmp.replace(" - ", "\n");
        }
    }

    private Card(int id, String text, String watermark, int numPick, int numDraw, boolean writeIn) {
        this.id = id;
        this.text = text;
        this.watermark = watermark;
        this.numPick = numPick;
        this.numDraw = numDraw;
        this.writeIn = writeIn;
    }

    public static Card newBlankCard() {
        return new Card(0, "", "", -1, -1, false);
    }

    public void setWinning(boolean winning) {
        this.winning = winning;
    }
}
