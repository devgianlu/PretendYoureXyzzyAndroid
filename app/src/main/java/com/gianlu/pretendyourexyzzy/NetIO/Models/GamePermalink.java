package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class GamePermalink implements Serializable {
    public final int gid;
    private final String gamePermalink;

    public GamePermalink(JSONObject obj) throws JSONException {
        this.gid = obj.getInt("gid");
        this.gamePermalink = obj.optString("gp", null);
    }

    public GamePermalink(int gid, JSONObject obj) {
        this.gid = gid;
        this.gamePermalink = obj.optString("gp", null);
    }

    @Nullable
    public static GamePermalink get(JSONObject obj) {
        try {
            return new GamePermalink(obj);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public String gamePermalink() {
        return gamePermalink;
    }
}
