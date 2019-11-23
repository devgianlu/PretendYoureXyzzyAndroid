package com.gianlu.pretendyourexyzzy.api.models;

import com.gianlu.pretendyourexyzzy.api.Cardcast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class CardcastDeck {
    public final String code;
    public final String name;
    public final Cardcast.Category category;
    public final boolean externalCopyright;
    public final long createdAt;
    public final long updatedAt;
    public final int calls;
    public final int responses;
    public final boolean hasNsfwCards;
    public final Author author;
    public final float rating;
    public final List<CardcastCard> sampleCalls;
    public final List<CardcastCard> sampleResponses;

    public CardcastDeck(JSONObject obj) throws JSONException, ParseException {
        code = obj.getString("code");
        name = obj.getString("name");
        category = Cardcast.Category.parse(obj.getString("category"));
        externalCopyright = obj.getBoolean("external_copyright");
        calls = obj.getInt("call_count");
        responses = obj.getInt("response_count");
        hasNsfwCards = obj.optBoolean("has_nsfw_cards", false);
        author = new Author(obj.getJSONObject("author"));
        rating = (float) obj.getDouble("rating");

        SimpleDateFormat parser = Cardcast.getDefaultDateParser();
        createdAt = parser.parse(obj.getString("created_at")).getTime();
        updatedAt = parser.parse(obj.getString("updated_at")).getTime();

        JSONArray sampleCallsArray = obj.optJSONArray("sample_calls");
        if (sampleCallsArray != null)
            sampleCalls = CardcastCard.toCardsList(code, sampleCallsArray);
        else sampleCalls = null;

        JSONArray sampleResponsesArray = obj.optJSONArray("sample_responses");
        if (sampleResponsesArray != null)
            sampleResponses = CardcastCard.toCardsList(code, sampleResponsesArray);
        else sampleResponses = null;
    }

    public class Author {
        public final String id;
        public final String username;

        public Author(JSONObject obj) throws JSONException {
            id = obj.getString("id");
            username = obj.getString("username");
        }
    }
}
