package com.gianlu.pretendyourexyzzy.NetIO.Models;


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
}
