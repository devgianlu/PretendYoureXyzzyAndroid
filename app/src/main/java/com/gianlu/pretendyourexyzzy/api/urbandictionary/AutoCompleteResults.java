package com.gianlu.pretendyourexyzzy.api.urbandictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AutoCompleteResults extends ArrayList<AutoCompleteResults.Item> {

    AutoCompleteResults(JSONArray array) throws JSONException {
        super(array.length());

        for (int i = 0; i < array.length(); i++)
            add(new Item(array.getJSONObject(i)));
    }

    public static class Item {
        public final String term;
        public final String preview;

        Item(JSONObject obj) throws JSONException {
            term = obj.getString("term");
            preview = obj.getString("preview");
        }
    }
}
