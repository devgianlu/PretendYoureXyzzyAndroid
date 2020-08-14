package com.gianlu.pretendyourexyzzy.api.crcast;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.List;

public final class CrCastUser {
    public final String username;
    public final String email;
    public final boolean moderator;
    public final boolean activated;
    public final boolean banned;
    public final long tokenExpiration;
    public final long registered;
    public final List<String> favorites;

    CrCastUser(@NonNull JSONObject obj) throws JSONException, ParseException {
        username = obj.getString("username");
        email = obj.getString("email");
        moderator = obj.getBoolean("moderator");
        activated = obj.getBoolean("activated");
        banned = obj.getBoolean("banned");
        tokenExpiration = CrCastApi.getApiDateTimeFormat().parse(obj.getString("tokenExpiration")).getTime();
        registered = CrCastApi.getApiDateTimeFormat().parse(obj.getString("registerdate")).getTime();
        favorites = CommonUtils.toStringsList(obj.getJSONArray("favorites"), false);
    }
}
