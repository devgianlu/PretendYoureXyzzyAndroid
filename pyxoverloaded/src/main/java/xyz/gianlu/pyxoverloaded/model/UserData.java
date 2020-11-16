package xyz.gianlu.pyxoverloaded.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gianlu.commonutils.CommonUtils;

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
    public final PurchaseStatusGranular purchaseStatusGranular;
    public final String username;
    public final Long expireTime;
    private final Map<PropertyKey, String> properties;

    public UserData(@NotNull JSONObject obj) throws JSONException {
        this.username = obj.getString("username");
        this.purchaseStatus = PurchaseStatus.parse(obj.getString("purchaseStatus"));
        this.purchaseStatusGranular = PurchaseStatusGranular.parse(obj.getString("purchaseStatusGranular"));
        this.expireTime = CommonUtils.optLong(obj, "expireTime");

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

    public enum PurchaseStatusGranular {
        ACTIVE("active", false),
        CANCELLED("cancelled", true),
        GRACE_PERIOD("gracePeriod", true),
        ACCOUNT_HOLD("accountHold", true),
        PAUSED("paused", true),
        EXPIRED("expired", false);

        public final boolean message;
        private final String val;

        PurchaseStatusGranular(@NotNull String val, boolean message) {
            this.val = val;
            this.message = message;
        }

        @NonNull
        private static PurchaseStatusGranular parse(@Nullable String val) {
            if (val == null) throw new IllegalArgumentException("Can't parse null value.");

            for (PurchaseStatusGranular status : values()) {
                if (Objects.equals(status.val, val))
                    return status;
            }

            throw new IllegalArgumentException("Unknown granular purchase status: " + val);
        }

        @StringRes
        public int getName() {
            switch (this) {
                case ACTIVE:
                    return R.string.active;
                case CANCELLED:
                    return R.string.cancelled;
                case GRACE_PERIOD:
                    return R.string.gracePeriod;
                case ACCOUNT_HOLD:
                    return R.string.accountHold;
                case PAUSED:
                    return R.string.paused;
                case EXPIRED:
                    return R.string.expired;
                default:
                    throw new IllegalArgumentException("Unknown state: " + this);
            }
        }

        @NotNull
        public String getMessage(@NotNull Context context, @Nullable Long expireTimeMillis) {
            switch (this) {
                case CANCELLED:
                    return context.getString(R.string.subscriptionCancelled_message, CommonUtils.getVerbalDateFormatter().format(expireTimeMillis));
                case GRACE_PERIOD:
                    return context.getString(R.string.subscriptionGracePeriod_message, CommonUtils.getVerbalDateFormatter().format(expireTimeMillis));
                case ACCOUNT_HOLD:
                    return context.getString(R.string.subscriptionAccountHold_message);
                case PAUSED:
                    return context.getString(R.string.subscriptionPaused_message);
                default:
                case ACTIVE:
                case EXPIRED:
                    throw new IllegalStateException();
            }
        }
    }

    public enum PurchaseStatus {
        OK("ok", true), NONE("none", false);

        public final boolean ok;
        private final String val;

        PurchaseStatus(@NonNull String val, boolean ok) {
            this.val = val;
            this.ok = ok;
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
    }
}
