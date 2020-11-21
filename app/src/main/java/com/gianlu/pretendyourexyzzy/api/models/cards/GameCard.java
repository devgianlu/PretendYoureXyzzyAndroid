package com.gianlu.pretendyourexyzzy.api.models.cards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameCard extends BaseCard {
    private static final Pattern CARD_NUM_PATTERN = Pattern.compile("<span class=\"cardnum\">(\\d*)\\s?[/\\\\]\\s?(\\d*)</span>");
    public final int id;
    public final int numPick;
    public final int numDraw;
    public final boolean writeIn;
    public final String originalText;
    public final String originalWatermark;
    private final String text;
    private final String watermark;
    private transient boolean winner = false;

    private GameCard(int id, @NonNull String originalText, @NonNull String originalWatermark, @NonNull JSONObject obj) {
        this.id = id;
        this.originalText = originalText.replace(" - ", "\n");
        this.originalWatermark = originalWatermark;

        numPick = obj.optInt("PK", -1);
        numDraw = obj.optInt("D", -1);
        writeIn = obj.optBoolean("wi", false);

        String textTmp = this.originalText;
        String watermarkTmp = this.originalWatermark;
        Matcher matcher = CARD_NUM_PATTERN.matcher(textTmp);
        if (matcher.find()) {
            text = textTmp.replace(matcher.group(), "").trim();
            watermark = watermarkTmp + " (" + matcher.group(1) + "/" + matcher.group(2) + ")";
        } else {
            watermark = watermarkTmp;
            text = textTmp.trim();
        }
    }

    @NonNull
    public static BaseCard parse(@NonNull JSONObject obj) throws JSONException {
        int id = obj.getInt("cid");
        String text = obj.getString("T");
        String watermark = obj.getString("W");
        if (id == -1 && text.isEmpty() && watermark.isEmpty()) return new UnknownCard();
        else return new GameCard(id, text, watermark, obj);
    }

    @NonNull
    public static List<BaseCard> list(@NonNull JSONArray array) throws JSONException {
        List<BaseCard> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) list.add(parse(array.getJSONObject(i)));
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameCard card = (GameCard) o;
        return id == card.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean black() {
        return numPick != -1;
    }

    @NonNull
    @Override
    public String text() {
        return text;
    }

    @Override
    @Nullable
    public String watermark() {
        return watermark;
    }

    @Override
    public int numPick() {
        return numPick;
    }

    @Override
    public int numDraw() {
        return numDraw;
    }

    public boolean isWinner() {
        return winner;
    }

    public static void setWinner(@NonNull CardsGroup group) {
        for (BaseCard card : group)
            if (card instanceof GameCard)
                ((GameCard) card).setWinner();
    }

    public void setWinner() {
        this.winner = true;
    }
}
