package com.gianlu.pretendyourexyzzy.api.urbandictionary;

import org.json.JSONException;
import org.json.JSONObject;

public class Definition {
    public final String word;
    public final String definition;
    public final int id;
    public final String example;
    public final String permalink;

    Definition(JSONObject obj) throws JSONException {
        word = obj.getString("word");
        definition = obj.getString("definition");
        id = obj.getInt("defid");
        permalink = obj.getString("permalink");
        example = obj.getString("example")
                .replace('\n', '\0')
                .replace('\n', '\0')
                .trim();
    }
}
