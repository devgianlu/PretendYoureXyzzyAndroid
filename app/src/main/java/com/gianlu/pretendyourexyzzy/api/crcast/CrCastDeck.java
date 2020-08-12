package com.gianlu.pretendyourexyzzy.api.crcast;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class CrCastDeck extends BasicCustomDeck {
    public final String desc;
    public final CrCastApi.State state;
    public final boolean privateDeck;
    public final String lang;
    public final List<CrCastCard> blacks;
    public final List<CrCastCard> whites;
    public final long created;

    CrCastDeck(@NonNull JSONObject obj) throws JSONException, ParseException {
        super(obj.getString("name"), obj.getString("deckcode"), null /* FIXME */, 0 /* TODO: Get from somewhere */, -1);
        desc = obj.getString("description");
        lang = obj.getString("decklang");
        state = CrCastApi.State.parse(obj.getInt("state"));
        privateDeck = obj.getBoolean("private");
        created = CrCastApi.getApiDateTimeFormat().parse(obj.getString("createdate")).getTime();

        JSONArray blacksArray = obj.getJSONArray("blacks");
        blacks = new ArrayList<>(blacksArray.length());
        for (int i = 0; i < blacksArray.length(); i++)
            blacks.add(new CrCastCard(watermark, true, blacksArray.getJSONObject(i)));

        JSONArray whitesArray = obj.getJSONArray("whites");
        whites = new ArrayList<>(whitesArray.length());
        for (int i = 0; i < whitesArray.length(); i++)
            whites.add(new CrCastCard(watermark, false, whitesArray.getJSONObject(i)));
    }

    @Override
    public int whiteCardsCount() {
        return whites.size();
    }

    @Override
    public int blackCardsCount() {
        return blacks.size();
    }
}
