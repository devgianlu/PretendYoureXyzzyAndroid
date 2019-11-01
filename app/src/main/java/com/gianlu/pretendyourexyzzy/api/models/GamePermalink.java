package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class GamePermalink implements Serializable {
    public final int gid;
    public String gamePermalink;

    public GamePermalink(JSONObject obj) throws JSONException {
        this.gid = obj.getInt("gid");
        this.gamePermalink = CommonUtils.optString(obj, "gp");
    }

    public GamePermalink(int gid, JSONObject obj) {
        this.gid = gid;
        this.gamePermalink = CommonUtils.optString(obj, "gp");
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
    public String extractGameMetricsId() {
        if (gamePermalink == null) return null;
        String[] split = gamePermalink.split("/");
        return split[split.length - 1];
    }
}
