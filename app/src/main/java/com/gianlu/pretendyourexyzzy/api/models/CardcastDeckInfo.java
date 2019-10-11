package com.gianlu.pretendyourexyzzy.api.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class CardcastDeckInfo extends CardcastDeck {
    public final String copyrightHolderUrl;
    public final String description;
    public final boolean unlisted;

    public CardcastDeckInfo(JSONObject obj) throws JSONException, ParseException {
        super(obj);

        copyrightHolderUrl = obj.optString("copyright_holder_url", null);
        description = obj.getString("description");
        unlisted = obj.getBoolean("unlisted");
    }
}
