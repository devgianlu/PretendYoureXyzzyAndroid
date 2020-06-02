package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import okhttp3.Request;
import xyz.gianlu.pyxoverloaded.callback.SuccessCallback;
import xyz.gianlu.pyxoverloaded.callback.SyncCallback;

import static xyz.gianlu.pyxoverloaded.TaskUtils.callbacks;
import static xyz.gianlu.pyxoverloaded.Utils.jsonBody;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;
import static xyz.gianlu.pyxoverloaded.Utils.singletonJsonBody;

public class OverloadedSyncApi {
    private static OverloadedSyncApi instance = null;
    private final OverloadedApi api;
    private final List<SyncStatusListener> listeners = new ArrayList<>(5);
    private final Handler handler;
    private final EnumMap<SyncProduct, SyncStatus> syncStatuses = new EnumMap<>(SyncProduct.class);

    private OverloadedSyncApi(@NonNull OverloadedApi api) {
        this.api = api;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public static OverloadedSyncApi get() {
        if (instance == null) instance = new OverloadedSyncApi(OverloadedApi.get());
        return instance;
    }

    public void addSyncListener(@NonNull SyncStatusListener listener) {
        listeners.add(listener);

        for (SyncProduct product : new ArrayList<>(syncStatuses.keySet())) {
            SyncStatus ss = syncStatuses.get(product);
            if (ss == null) continue;

            handler.post(() -> listener.syncStatusUpdated(product, ss.isSyncing, ss.error));
        }
    }

    public void removeSyncListener(@NonNull SyncStatusListener listener) {
        listeners.remove(listener);
    }

    private void dispatchSyncUpdate(@NonNull SyncProduct product, boolean isSyncing, boolean error) {
        syncStatuses.put(product, new SyncStatus(isSyncing, error));
        for (SyncStatusListener listener : new ArrayList<>(listeners))
            handler.post(() -> listener.syncStatusUpdated(product, isSyncing, error));
    }


    /////////////////////////////////////////
    ///////////// Starred cards /////////////
    /////////////////////////////////////////

    public void syncStarredCards(long localRevision, @Nullable Activity activity, @NonNull SyncCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/StarredCards"))
                    .post(singletonJsonBody("revision", localRevision)));
            return new SyncResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, System.currentTimeMillis());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, result.needsUpdate || result.update != null, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    public void updateStarredCards(long revision, @NonNull JSONArray update, @Nullable Activity activity, @NonNull SuccessCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

            JSONObject body = new JSONObject();
            body.put("revision", revision);
            body.put("update", update);

            api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/UpdateStarredCards"))
                    .post(jsonBody(body)));
            return null;
        }), activity, (OnSuccessListener<Void>) v -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, System.currentTimeMillis());
            callback.onSuccessful();
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    public void patchStarredCards(long revision, @NonNull PatchOp op, @NonNull JSONObject item, @Nullable Activity activity, @NonNull SuccessCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

            JSONObject body = new JSONObject();
            body.put("revision", revision);
            body.put("patch", new JSONObject()
                    .put("type", op.name())
                    .put("item", item));

            api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/UpdateStarredCards"))
                    .post(jsonBody(body)));
            return null;
        }), activity, (OnSuccessListener<Void>) v -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, System.currentTimeMillis());
            callback.onSuccessful();
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    public enum SyncProduct {
        STARRED_CARDS, CUSTOM_DECKS
    }

    public enum PatchOp {
        ADD, REM
    }

    public interface SyncStatusListener {
        void syncStatusUpdated(@NonNull SyncProduct product, boolean isSyncing, boolean error);
    }

    private static class SyncStatus {
        final boolean isSyncing;
        final boolean error;

        SyncStatus(boolean isSyncing, boolean error) {
            this.isSyncing = isSyncing;
            this.error = error;
        }
    }

    public static class SyncResponse {
        public final boolean needsUpdate;
        public final JSONArray update;
        public final Long revision;

        private SyncResponse(@NonNull JSONObject resp) throws JSONException {
            needsUpdate = resp.getBoolean("needsUpdate");
            update = resp.optJSONArray("update");
            revision = CommonUtils.optLong(resp, "revision");
        }
    }
}
