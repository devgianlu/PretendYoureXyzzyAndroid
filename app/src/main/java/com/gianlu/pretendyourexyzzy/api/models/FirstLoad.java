package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class FirstLoad {
    public final NextOp nextOperation;
    public final boolean inProgress;
    public final List<Deck> decks;
    public final User user;
    public final GamePermalink game;

    public FirstLoad(@NonNull JSONObject obj, @Nullable User user) throws JSONException {
        this.nextOperation = NextOp.parse(obj.getString("next"));
        this.inProgress = obj.getBoolean("ip");
        this.decks = Deck.list(obj.getJSONArray("css"), false);
        this.user = user;
        this.game = GamePermalink.get(obj);
    }

    @Nullable
    public String cardSetName(int id) {
        Deck set = Deck.findDeck(decks, id);
        return set == null ? null : set.name;
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
