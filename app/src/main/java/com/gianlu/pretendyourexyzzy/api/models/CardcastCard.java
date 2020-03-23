package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.api.Cardcast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardcastCard extends BaseCard {
    public final String id;
    public final List<String> text;
    public final long createdAt;
    public final boolean nsfw;
    public final String deckCode;

    public CardcastCard(String deckCode, JSONObject obj) throws ParseException, JSONException {
        this.deckCode = deckCode;
        id = obj.getString("id");
        nsfw = obj.getBoolean("nsfw");

        Date createdAtDate = Cardcast.getDefaultDateParser().parse(obj.getString("created_at"));
        if (createdAtDate != null) createdAt = createdAtDate.getTime();
        else createdAt = 0;

        text = new ArrayList<>();
        JSONArray textArray = obj.getJSONArray("text");
        for (int i = 0; i < textArray.length(); i++)
            text.add(textArray.getString(i));
    }

    @NonNull
    public static List<CardcastCard> toCardsList(String code, JSONArray array) throws JSONException, ParseException {
        List<CardcastCard> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++)
            list.add(new CardcastCard(code, array.getJSONObject(i)));
        return list;
    }

    @Override
    @NonNull
    public String text() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (String split : text) {
            if (!first) builder.append(" ____ ");
            first = false;
            builder.append(split);
        }

        return builder.toString();
    }

    @Override
    @Nullable
    public String watermark() {
        return deckCode;
    }

    @Override
    public int numPick() {
        if (text.size() == 1) return -1;
        return text.size() - 1;
    }

    @Override
    public int numDraw() {
        return 0;
    }

    @Override
    public int id() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardcastCard that = (CardcastCard) o;
        return id.equals(that.id);
    }

    @Override
    public boolean unknown() {
        return false;
    }

    @Override
    public boolean black() {
        return text.size() > 1;
    }

    @Override
    public boolean writeIn() {
        return false;
    }

    @Override
    @Nullable
    public JSONObject toJson() {
        return null;
    }
}
