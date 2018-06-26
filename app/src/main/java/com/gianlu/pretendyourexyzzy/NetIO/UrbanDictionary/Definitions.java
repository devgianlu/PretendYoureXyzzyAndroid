package com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Definitions extends ArrayList<Definition> {

    Definitions(JSONObject obj) throws JSONException {
        JSONArray array = obj.getJSONArray("list");
        for (int i = 0; i < array.length(); i++)
            add(new Definition(array.getJSONObject(i)));
    }
}
