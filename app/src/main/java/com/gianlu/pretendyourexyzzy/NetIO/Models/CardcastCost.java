package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONException;
import org.json.JSONObject;

public class CardcastCost {
    /**
     * the cost per unit in USD
     */
    public final float unitCost;

    /**
     * the number of calls (black cards) to print
     */
    public final int callsCount;

    /**
     * the number of responses (white cards) to print
     */
    public final int responsesCount;

    public CardcastCost(JSONObject obj) throws JSONException {
        unitCost = (float) obj.getDouble("unit");
        callsCount = obj.getInt("print_call_count");
        responsesCount = obj.getInt("print_response_count");
    }
}
