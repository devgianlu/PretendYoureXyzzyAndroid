package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class User implements BaseDrawerProfile, Serializable {
    public final String nickname;
    public final String sessionId;

    public User(@NonNull String sessionId, JSONObject obj) throws JSONException {
        this.sessionId = sessionId;
        this.nickname = obj.getString("n");
    }

    @Override
    public String getProfileName(Context context) {
        return nickname;
    }

    @Override
    public String getSecondaryText(Context context) {
        return null;
    }

    @Override
    public String getInitials(Context context) {
        return nickname.substring(0, 2);
    }
}
