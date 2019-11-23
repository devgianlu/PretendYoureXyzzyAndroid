package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FirstLoad {
    public final NextOp nextOperation;
    public final boolean inProgress;
    public final List<Deck> decks;
    public final User user;
    public final GamePermalink game;

    public FirstLoad(JSONObject obj, @Nullable User user) throws JSONException {
        this.nextOperation = NextOp.parse(obj.getString("next"));
        this.inProgress = obj.getBoolean("ip");
        this.decks = Deck.list(obj.getJSONArray("css"));
        this.user = user;
        this.game = GamePermalink.get(obj);
    }

    @NonNull
    public List<String> createCardSetNamesList(List<Integer> includeIds) {
        List<String> names = new ArrayList<>();
        for (int id : includeIds) {
            Deck set = Utils.findCardSet(decks, id);
            if (set != null) names.add(set.name);
        }

        return names;
    }

    public enum NextOp {
        REGISTER("r"),
        GAME("game"),
        NONE("none");

        private final String val;

        NextOp(String val) {
            this.val = val;
        }

        @NonNull
        public static NextOp parse(String val) {
            for (NextOp op : values())
                if (Objects.equals(op.val, val))
                    return op;

            throw new IllegalArgumentException("Cannot find operation with value: " + val);
        }
    }
}
