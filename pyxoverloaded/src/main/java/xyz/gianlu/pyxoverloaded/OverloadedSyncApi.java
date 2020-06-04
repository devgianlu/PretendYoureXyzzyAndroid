package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import okhttp3.Request;
import xyz.gianlu.pyxoverloaded.callback.CustomDecksSyncCallback;
import xyz.gianlu.pyxoverloaded.callback.StarredCardsSyncCallback;
import xyz.gianlu.pyxoverloaded.callback.UpdateSyncCallback;

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

    public void syncStarredCards(long localRevision, @Nullable Activity activity, @NonNull StarredCardsSyncCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/StarredCards"))
                    .post(singletonJsonBody("rev", localRevision)));
            return new StarredCardsSyncResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, System.currentTimeMillis());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, result.needsUpdate, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    public void updateStarredCards(long revision, @NonNull JSONArray update, @Nullable Activity activity, @NonNull UpdateSyncCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("update", update);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/UpdateStarredCards"))
                    .post(jsonBody(body)));
            return new UpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, System.currentTimeMillis());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    @Contract("_, _, null, null, _, _ -> fail")
    public void patchStarredCards(long revision, @NonNull PatchOp op, @Nullable Long remoteId, @Nullable JSONObject item, @Nullable Activity activity, @NonNull UpdateSyncCallback callback) {
        if (remoteId == null && item == null)
            throw new IllegalArgumentException();

        callbacks(Tasks.call(api.executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

            JSONObject body = new JSONObject();
            body.put("revision", revision);
            body.put("patch", new JSONObject()
                    .put("type", op.name())
                    .put("remoteId", remoteId)
                    .put("item", item));

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/UpdateStarredCards"))
                    .post(jsonBody(body)));
            return new UpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, System.currentTimeMillis());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }


    /////////////////////////////////////////
    ///////////// Custom decks //////////////
    /////////////////////////////////////////

    public void syncCustomDecks(@NonNull JSONObject revisions, @Nullable JSONArray local, @Nullable Activity activity, @NonNull CustomDecksSyncCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("revisions", revisions)
                    .put("local", local);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/CustomDecks"))
                    .post(jsonBody(body)));

            List<CustomDeckSyncResponse> list = new ArrayList<>(obj.length());
            Iterator<String> iter = obj.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                long id = Long.parseLong(key);
                JSONObject deckRev = obj.getJSONObject(key);
                list.add(new CustomDeckSyncResponse(id, deckRev.getBoolean("needsUpdate"), deckRev.optJSONObject("update")));
            }
            return list;
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, System.currentTimeMillis());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, CustomDeckSyncResponse.needsSomeUpdates(result), false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
        });
    }

    public void updateCustomDeck(long revision, @NonNull JSONObject update, @Nullable Activity activity, @NonNull UpdateSyncCallback callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("update", update);

            JSONObject obj = api.serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Sync/UpdateCustomDecks"))
                    .post(jsonBody(body)));
            return new UpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, System.currentTimeMillis());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
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

    public static class UpdateResponse {
        public final Long remoteId;
        public final long[] remoteIds;

        public UpdateResponse(@NonNull JSONObject obj) throws JSONException {
            remoteId = CommonUtils.optLong(obj, "remoteId");

            if (!obj.has("remoteIds") || obj.isNull("remoteIds")) {
                remoteIds = null;
            } else {
                JSONArray array = obj.getJSONArray("remoteIds");
                remoteIds = new long[array.length()];
                for (int i = 0; i < array.length(); i++)
                    remoteIds[i] = array.getLong(i);
            }
        }
    }

    public static class CustomDeckSyncResponse {
        public final long remoteId;
        public final boolean needsUpdate;
        public final JSONObject update;

        private CustomDeckSyncResponse(long remoteId, boolean needsUpdate, @Nullable JSONObject update) {
            this.remoteId = remoteId;
            this.needsUpdate = needsUpdate;
            this.update = update;
        }

        private static boolean needsSomeUpdates(@NonNull List<CustomDeckSyncResponse> list) {
            for (CustomDeckSyncResponse deck : list)
                if (deck.needsUpdate)
                    return true;

            return false;
        }
    }

    public static class StarredCardsSyncResponse {
        public final boolean needsUpdate;
        public final JSONArray update;
        public final Long revision;

        private StarredCardsSyncResponse(@NonNull JSONObject resp) throws JSONException {
            needsUpdate = resp.getBoolean("needsUpdate");
            update = resp.optJSONArray("update");
            revision = CommonUtils.optLong(resp, "revision");
        }
    }
}
