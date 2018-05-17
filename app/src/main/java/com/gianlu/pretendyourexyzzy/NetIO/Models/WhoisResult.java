package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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

        ipAddr = obj.optString("IP", null);
        client = obj.optString("cn", null);
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
