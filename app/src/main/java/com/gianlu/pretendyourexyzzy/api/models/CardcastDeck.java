package com.gianlu.pretendyourexyzzy.api.models;

import com.gianlu.pretendyourexyzzy.api.Cardcast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    CardcastDeck(JSONObject obj) throws JSONException, ParseException {
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
        Date createdAtDate = parser.parse(obj.getString("created_at"));
        if (createdAtDate != null) createdAt = createdAtDate.getTime();
        else createdAt = 0;

        Date updatedAtDate = parser.parse(obj.getString("updated_at"));
        if (updatedAtDate != null) updatedAt = updatedAtDate.getTime();
        else updatedAt = 0;

        JSONArray sampleCallsArray = obj.optJSONArray("sample_calls");
        if (sampleCallsArray == null) sampleCalls = null;
        else sampleCalls = CardcastCard.toCardsList(code, sampleCallsArray);

        JSONArray sampleResponsesArray = obj.optJSONArray("sample_responses");
        if (sampleResponsesArray == null) sampleResponses = null;
        else sampleResponses = CardcastCard.toCardsList(code, sampleResponsesArray);
    }

    public static class Author {
        public final String id;
        public final String username;

        Author(JSONObject obj) throws JSONException {
            id = obj.getString("id");
            username = obj.getString("username");
        }
    }
}
