package com.gianlu.pretendyourexyzzy.NetIO.Models;

import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class CardcastCard implements BaseCard {
    public final String id;
    public final List<String> text;
    public final long createdAt;
    public final boolean nsfw;
    public final String deckCode;

    public CardcastCard(String deckCode, JSONObject obj) throws ParseException, JSONException {
        this.deckCode = deckCode;
        id = obj.getString("id");
        createdAt = Cardcast.getDefaultDateParser().parse(obj.getString("created_at")).getTime();
        nsfw = obj.getBoolean("nsfw");

        text = new ArrayList<>();
        JSONArray textArray = obj.getJSONArray("text");
        for (int i = 0; i < textArray.length(); i++)
            text.add(textArray.getString(i));
    }

    public static List<CardcastCard> toCardsList(String code, JSONArray array) throws JSONException, ParseException {
        List<CardcastCard> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++)
            list.add(new CardcastCard(code, array.getJSONObject(i)));
        return list;
    }

    @Override
    public String getText() {
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
    public String getWatermark() {
        return deckCode;
    }

    @Override
    public int getNumPick() {
        if (text.size() == 1) return -1;
        return text.size() - 1;
    }

    @Override
    public int getNumDraw() {
        return 0;
    }

    @Override
    public int getId() {
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
    public boolean isUnknown() {
        return false;
    }

    @Override
    public boolean isBlack() {
        return text.size() > 1;
    }
}
