package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class FriendStatus {
    public final String username;
    public final boolean mutual;
    public String serverId;

    private FriendStatus(@NotNull String username, @NotNull JSONObject obj) throws JSONException {
        this.username = username;
        mutual = obj.getBoolean("mutual");
        serverId = CommonUtils.optString(obj, "loggedServer");
    }

    @NonNull
    public static Map<String, FriendStatus> parse(@NonNull JSONObject obj) throws JSONException {
        Map<String, FriendStatus> map = new HashMap<>();
        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) {
            String username = iter.next();
            map.put(username, new FriendStatus(username, obj.getJSONObject(username)));
        }

        return map;
    }

    @Nullable
    public String server() {
        return serverId;
    }

    public void update(@Nullable String serverId) {
        this.serverId = serverId;
    }

    @Override
    public String toString() {
        return "FriendStatus{" + "username='" + username + '\'' + ", mutual=" + mutual + '}';
    }
}
