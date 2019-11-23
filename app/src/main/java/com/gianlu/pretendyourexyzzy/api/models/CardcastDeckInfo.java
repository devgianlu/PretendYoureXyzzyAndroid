package com.gianlu.pretendyourexyzzy.api.models;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class CardcastDeckInfo extends CardcastDeck {
    public final String copyrightHolderUrl;
    public final String description;
    public final boolean unlisted;

    public CardcastDeckInfo(JSONObject obj) throws JSONException, ParseException {
        super(obj);

        copyrightHolderUrl = CommonUtils.optString(obj, "copyright_holder_url");
        description = obj.getString("description");
        unlisted = obj.getBoolean("unlisted");
    }
}
