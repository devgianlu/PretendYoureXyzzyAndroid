package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FriendStatus {
    public final String username;
    public final boolean mutual;
    public final boolean request;
    public String serverId;

    public FriendStatus(@NotNull JSONObject obj) throws JSONException {
        username = obj.getString("username");
        mutual = obj.getBoolean("mutual");
        serverId = CommonUtils.optString(obj, "loggedServer");
        request = false;
    }

    private FriendStatus(@NonNull String username, boolean mutual, boolean request, @Nullable String serverId) {
        this.username = username;
        this.mutual = mutual;
        this.request = request;
        this.serverId = serverId;

        if (request && mutual) throw new IllegalStateException();
    }

    @NonNull
    public static Map<String, FriendStatus> parse(@NonNull JSONObject obj) throws JSONException {
        JSONArray friendsArray = obj.getJSONArray("friends");
        JSONArray requestsArray = obj.getJSONArray("requests");

        Map<String, FriendStatus> map = new HashMap<>(friendsArray.length() + requestsArray.length());
        for (int i = 0; i < requestsArray.length(); i++) {
            String username = requestsArray.getString(i);
            map.put(username, new FriendStatus(username, false, true, null));
        }

        for (int i = 0; i < friendsArray.length(); i++) {
            FriendStatus status = new FriendStatus(friendsArray.getJSONObject(i));
            map.put(status.username, status);
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

    @NotNull
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
    public FriendStatus asRequest() {
        return new FriendStatus(username, false, true, null);
    }
}
