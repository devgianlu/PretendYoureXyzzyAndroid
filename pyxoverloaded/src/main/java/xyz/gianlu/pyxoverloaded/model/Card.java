package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private static final int CARD_TYPE_BLACK = 0;
    private static final int CARD_TYPE_WHITE = 1;
    public final String text;
    public final String watermark;
    public final int type;

    public Card(@NonNull JSONObject obj) throws JSONException {
        text = obj.getString("text");
        watermark = obj.getString("watermark");
        type = obj.getInt("type");
    }

    @NonNull
    public static List<Card> parse(@NonNull JSONArray array) throws JSONException {
        List<Card> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) list.add(new Card(array.getJSONObject(i)));
        return list;
    }

    public boolean black() {
        return type == CARD_TYPE_BLACK;
    }
}
