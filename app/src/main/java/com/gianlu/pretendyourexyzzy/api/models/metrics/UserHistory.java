package com.gianlu.pretendyourexyzzy.api.models.metrics;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UserHistory extends ArrayList<UserHistory.Session> {

    public UserHistory(JSONObject obj) throws JSONException {
        JSONArray array = obj.getJSONArray("Sessions");
        for (int i = 0; i < array.length(); i++) add(new Session(array.getJSONObject(i)));
    }

    public class Session {
        public final String id;
        public final long loginTimestamp;

        Session(JSONObject obj) throws JSONException {
            id = obj.getString("SessionId");
            loginTimestamp = obj.getLong("LogInTimestamp") * 1000;
        }

        @NonNull
        public String name() {
            return id;
        }
    }
}
