package xyz.gianlu.pyxoverloaded.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.R;

public class UserData {
    public final PurchaseStatus purchaseStatus;
    public final String purchaseToken;
    public final String username;
    public final List<String> friends;

    public UserData(@NotNull JSONObject obj) throws JSONException {
        this.username = obj.getString("username");
        this.purchaseStatus = PurchaseStatus.parse(obj.getString("purchaseStatus"));
        this.purchaseToken = obj.getString("purchaseToken");
        this.friends = CommonUtils.toStringsList(obj.getJSONArray("friends"), false);
    }

    @NotNull
    @Override
    public String toString() {
        return "UserData{" +
                "purchaseStatus=" + purchaseStatus +
                ", purchaseToken='" + purchaseToken + '\'' +
                ", username='" + username + '\'' +
                '}';
    }

    public boolean hasUsername() {
        return username != null && !username.isEmpty();
    }

    public enum PurchaseStatus {
        NONE("none"), OK("ok"), PENDING("pending");

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
                case NONE:
                    res = R.string.none;
                    break;
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
