package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class FirstLoad implements Serializable {
    public final NextOp nextOperation;
    public final boolean inProgress;
    public final ArrayList<CardSet> cardSets;
    public final String nickname;

    public FirstLoad(JSONObject obj) throws JSONException {
        nextOperation = NextOp.parse(obj.getString("next"));
        inProgress = obj.getBoolean("ip");
        nickname = obj.optString("nickname", null);

        cardSets = new ArrayList<>();
        JSONArray cardSetsArray = obj.getJSONArray("css");
        for (int i = 0; i < cardSetsArray.length(); i++)
            cardSets.add(new CardSet(cardSetsArray.getJSONObject(i)));
    }

    public enum NextOp {
        REGISTER("r"),
        GAME("game"),
        NONE("none");

        @NonNull
        public static NextOp parse(String val) {
            for (NextOp op : values())
                if (Objects.equals(op.val, val))
                    return op;

            throw new IllegalArgumentException("Cannot find operation with value: " + val);
        }

        private final String val;

        NextOp(String val) {
            this.val = val;
        }
    }
}
