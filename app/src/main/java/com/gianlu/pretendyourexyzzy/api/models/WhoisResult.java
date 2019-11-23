package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static com.gianlu.commonutils.CommonUtils.optString;

public class WhoisResult extends UserInfo implements Serializable {
    public final long connectedAt;
    public final long idle;
    private final Game game;
    private final String ipAddr;
    private final String client;

    public WhoisResult(JSONObject obj) throws JSONException {
        super(obj);

        connectedAt = obj.getLong("ca");
        idle = obj.getLong("idl");

        if (CommonUtils.isStupidNull(obj, "gi")) game = null;
        else game = new Game(obj.getJSONObject("gi"));

        ipAddr = optString(obj, "IP");
        client = optString(obj, "cn");
    }

    @Nullable
    public Game game() {
        return game;
    }

    @Nullable
    public String ipAddress() {
        return ipAddr;
    }

    @Nullable
    public String clientName() {
        return client;
    }
}
