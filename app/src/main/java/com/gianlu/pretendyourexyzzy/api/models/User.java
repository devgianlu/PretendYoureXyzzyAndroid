package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class User extends UserInfo {
    public final String sessionId;
    public final String persistentId;
    private final String userPermalink;
    private final String sessionPermalink;

    public User(@NonNull String sessionId, JSONObject obj) throws JSONException {
        super(obj);
        this.sessionId = sessionId;
        this.persistentId = obj.getString("pid");
        this.userPermalink = CommonUtils.optString(obj, "up");
        this.sessionPermalink = CommonUtils.optString(obj, "sP");
    }

    @Nullable
    public String userPermalink() {
        return userPermalink;
    }

    @Nullable
    public String extractSessionMetricsId() {
        if (sessionPermalink == null) return null;
        String[] split = sessionPermalink.split("/");
        return split[split.length - 1];
    }

    @Nullable
    public String sessionPermalink() {
        return sessionPermalink;
    }
}
