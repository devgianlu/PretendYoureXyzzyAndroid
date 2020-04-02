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

    public FriendStatus(@NotNull JSONObject obj) throws JSONException {
        username = obj.getString("username");
        mutual = obj.getBoolean("mutual");
        serverId = CommonUtils.optString(obj, "loggedServer");
    }

    private FriendStatus(@NotNull String username, @NotNull JSONObject obj) throws JSONException {
        this.username = username;
        mutual = obj.getBoolean("mutual");
        serverId = CommonUtils.optString(obj, "loggedServer");
    }

    private FriendStatus(@NonNull String username, boolean mutual, @Nullable String serverId) {
        this.username = username;
        this.mutual = mutual;
        this.serverId = serverId;
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

    public void updateLoggedServer(@Nullable String serverId) {
        this.serverId = serverId;
    }

    @Override
    public String toString() {
        return "FriendStatus{" + "username='" + username + '\'' + ", mutual=" + mutual + '}';
    }

    @NonNull
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("mutual", mutual);
        obj.put("username", username);
        if (serverId != null) obj.put("loggedServer", serverId);
        return obj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendStatus status = (FriendStatus) o;
        return username.equals(status.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @NonNull
    public FriendStatus notMutual() {
        return new FriendStatus(username, false, null);
    }
}
