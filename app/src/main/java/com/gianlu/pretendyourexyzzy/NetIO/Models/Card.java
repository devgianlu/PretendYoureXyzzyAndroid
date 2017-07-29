package com.gianlu.pretendyourexyzzy.NetIO.Models;


import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;

public class Card {
    public final int id;
    public final String text;
    public final String watermark;
    public final int numPick;
    public final int numDraw;
    public final boolean writeIn;

    public Card(JSONObject obj) throws JSONException {
        id = obj.getInt("cid");
        text = obj.getString("T");
        watermark = obj.getString("W");
        numPick = obj.optInt("PK", -1);
        numDraw = obj.optInt("D", -1);
        writeIn = obj.optBoolean("wi", false);
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

    @SuppressWarnings("deprecation")
    public String getEscapedText() {
        return Html.fromHtml(text.replace(" - ", "\n")).toString();
    }
}
