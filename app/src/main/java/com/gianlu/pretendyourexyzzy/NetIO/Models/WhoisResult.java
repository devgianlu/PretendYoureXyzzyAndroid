package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class WhoisResult extends UserInfo implements Serializable {
    public final long connectedAt;
    public final long idle;

    // TODO: Other stuff (game, admin stuff)

    public WhoisResult(JSONObject obj) throws JSONException {
        super(obj);

        connectedAt = obj.getLong("ca");
        idle = obj.getLong("idl");
    }
}
