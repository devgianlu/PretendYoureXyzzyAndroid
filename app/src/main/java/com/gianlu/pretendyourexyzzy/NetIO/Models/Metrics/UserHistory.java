package com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics;

import com.gianlu.pretendyourexyzzy.NetIO.Pyx;

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
        public final Pyx.Server server;

        Session(JSONObject obj) throws JSONException {
            id = obj.getString("SessionId");
            loginTimestamp = obj.getLong("LogInTimestamp") * 1000;

            server = Pyx.Server.fromSessionId(id);
        }
    }
}
