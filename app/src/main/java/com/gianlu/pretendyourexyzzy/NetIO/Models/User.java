package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class User extends UserInfo implements Serializable {
    public final String sessionId;
    public final String persistentId;

    public User(@NonNull String sessionId, JSONObject obj) throws JSONException {
        super(obj);
        this.sessionId = sessionId;
        this.persistentId = obj.getString("pid");
    }
}
