package xyz.gianlu.pyxoverloaded.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.R;

public class UserData {
    private static final String TAG = UserData.class.getSimpleName();
    public final PurchaseStatus purchaseStatus;
    public final String purchaseToken;
    public final String username;
    private final Map<PropertyKey, String> properties;

    public UserData(@NotNull JSONObject obj) throws JSONException {
        this.username = obj.getString("username");
        this.purchaseStatus = PurchaseStatus.parse(obj.getString("purchaseStatus"));
        this.purchaseToken = obj.getString("purchaseToken");

        JSONObject propertiesObj = obj.getJSONObject("properties");
        this.properties = new HashMap<>(propertiesObj.length());
        Iterator<String> iter = propertiesObj.keys();
        while (iter.hasNext()) {
            String keyStr = iter.next();
            PropertyKey key = PropertyKey.parse(keyStr);
            if (key == null) {
                Log.w(TAG, "Unknown key: " + keyStr);
                continue;
            }

            this.properties.put(key, propertiesObj.getString(keyStr));
        }
    }

    public boolean getPropertyBoolean(@NonNull PropertyKey key) {
        return "true".equals(getProperty(key));
    }

    @Nullable
    public String getProperty(@NonNull PropertyKey key) {
        return properties.get(key);
    }

    @Nullable
    public String getPropertyOrDefault(@NonNull PropertyKey key, @NonNull String fallback) {
        String val = getProperty(key);
        return val == null ? fallback : val;
    }

    @NotNull
    @Override
    public String toString() {
        return "UserData{" +
                "purchaseStatus=" + purchaseStatus +
                ", purchaseToken='" + purchaseToken + '\'' +
                ", username='" + username + '\'' +
                ", properties=" + properties +
                '}';
    }

    public enum PropertyKey {
        PUBLIC_CUSTOM_DECKS("public_custom_decks"), PUBLIC_STARRED_CARDS("public_starred_cards");

        public final String val;

        PropertyKey(@NotNull String val) {
            this.val = val;
        }

        @Nullable
        public static PropertyKey parse(@NotNull String str) {
            for (PropertyKey key : values())
                if (key.val.equals(str))
                    return key;

            return null;
        }
    }

    public enum PurchaseStatus {
        OK("ok"), PENDING("pending");

        private final String val;

        PurchaseStatus(String val) {
            this.val = val;
        }

        @NonNull
        private static PurchaseStatus parse(@Nullable String val) {
            if (val == null) throw new IllegalArgumentException("Can't parse null value.");

            for (PurchaseStatus status : values()) {
                if (Objects.equals(status.val, val))
                    return status;
            }

            throw new IllegalArgumentException("Unknown purchaseStatus: " + val);
        }

        @NonNull
        public String toString(@NonNull Context context) {
            int res;
            switch (this) {
                case OK:
                    res = R.string.ok;
                    break;
                case PENDING:
                    res = R.string.pending;
                    break;
                default:
                    res = R.string.unknown;
                    break;
            }

            return context.getString(res);
        }
    }
}
