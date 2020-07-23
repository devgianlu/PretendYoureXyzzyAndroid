package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.gms.common.util.concurrent.NamedThreadFactory;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

import static com.gianlu.commonutils.CommonUtils.singletonJsonObject;
import static xyz.gianlu.pyxoverloaded.TaskUtils.callbacks;

public class OverloadedSyncApi {
    private static OverloadedSyncApi instance = null;
    private final OverloadedApi api;
    private final List<SyncStatusListener> listeners = new ArrayList<>(5);
    private final Handler handler;
    private final EnumMap<SyncProduct, SyncStatus> syncStatuses = new EnumMap<>(SyncProduct.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("overloaded-sync-api"));

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

    public void syncStarredCards(long revision, @Nullable Activity activity, @NonNull GeneralCallback<StarredCardsSyncResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = api.makePostRequest("Sync/StarredCards", singletonJsonObject("rev", revision));
            return new StarredCardsSyncResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, result.needsUpdate, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    public void updateStarredCards(long revision, @NonNull JSONArray update, @Nullable Activity activity, @NonNull GeneralCallback<StarredCardsUpdateResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("update", update);

            JSONObject obj = api.makePostRequest("Sync/UpdateStarredCards", body);
            return new StarredCardsUpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
        });
    }

    @Contract("_, _, null, null, _, _ -> fail")
    public void patchStarredCards(long revision, @NonNull StarredCardsPatchOp op, @Nullable Long remoteId, @Nullable JSONObject item, @Nullable Activity activity, @NonNull GeneralCallback<StarredCardsUpdateResponse> callback) {
        if (remoteId == null && item == null)
            throw new IllegalArgumentException();

        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("patch", new JSONObject()
                    .put("type", op.name())
                    .put("remoteId", remoteId)
                    .put("item", item));

            JSONObject obj = api.makePostRequest("Sync/UpdateStarredCards", body);
            return new StarredCardsUpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, OverloadedApi.now());
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

    public void syncCustomDecks(@NonNull JSONArray syncItems, @Nullable Activity activity, @NonNull GeneralCallback<List<CustomDecksSyncResponse>> callback) {
        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("items", syncItems);

            JSONObject obj = api.makePostRequest("Sync/CustomDecks", body);

            JSONArray items = obj.getJSONArray("items");
            List<CustomDecksSyncResponse> list = new ArrayList<>(items.length());
            for (int i = 0; i < items.length(); i++)
                list.add(new CustomDecksSyncResponse(items.getJSONObject(i)));
            return list;
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, CustomDecksSyncResponse.needsSomeUpdates(result), false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
        });
    }

    public void updateCustomDeck(long revision, @NonNull JSONObject update, @Nullable Activity activity, @NonNull GeneralCallback<CustomDecksUpdateResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("update", update);

            JSONObject obj = api.makePostRequest("Sync/UpdateCustomDecks", body);
            return new CustomDecksUpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
        });
    }

    public void patchCustomDeck(long revision, @Nullable Long remoteId, @NonNull CustomDecksPatchOp op, @Nullable JSONObject deck, @Nullable JSONObject card, @Nullable Long cardId, @Nullable JSONArray cards, @Nullable Activity activity, @NonNull GeneralCallback<CustomDecksUpdateResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("patch", new JSONObject()
                    .put("id", remoteId)
                    .put("relId", cardId)
                    .put("type", op.name())
                    .put("deck", deck)
                    .put("cards", cards)
                    .put("card", card));

            JSONObject obj = api.makePostRequest("Sync/UpdateCustomDecks", body);
            return new CustomDecksUpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
        });
    }

    public void getPublicCustomDeck(@NonNull String username, @NonNull String name, @Nullable Activity activity, @NonNull GeneralCallback<UserProfile.CustomDeckWithCards> callback) {
        callbacks(Tasks.call(api.executorService /* Using main executor because this is not sensible to concurrency */, () -> {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("name", name);
            JSONObject obj = api.makePostRequest("Sync/GetPublicCustomDeck", body);
            return new UserProfile.CustomDeckWithCards(obj);
        }), activity, callback::onResult, callback::onFailed);
    }

    public void searchPublicCustomDeck(@NonNull String name, @NonNull String watermark, @NonNull String desc, int blackCards, int whiteCards, @Nullable Activity activity, @NonNull GeneralCallback<UserProfile.CustomDeckWithCards> callback) {
        callbacks(Tasks.call(api.executorService /* Using main executor because this is not sensible to concurrency */, () -> {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("watermark", watermark);
            body.put("desc", desc);
            body.put("blackCards", blackCards);
            body.put("whiteCards", whiteCards);
            JSONObject obj = api.makePostRequest("Sync/SearchPublicCustomDeck", body);
            return new UserProfile.CustomDeckWithCards(obj);
        }), activity, callback::onResult, callback::onFailed);
    }


    /////////////////////////////////////////
    ///////// Stared custom decks ///////////
    /////////////////////////////////////////

    public void syncStarredCustomDecks(long revision, @Nullable Activity activity, @NonNull GeneralCallback<StarredCustomDecksSyncResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, true, false);
            JSONObject obj = api.makePostRequest("Sync/StarredCustomDecks", singletonJsonObject("rev", revision));
            return new StarredCustomDecksSyncResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, result.needsUpdate, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, true);
        });
    }

    @Contract("_, _, null, null, _, _ -> fail")
    public void patchStarredCustomDecks(long revision, @NonNull StarredCustomDecksPatchOp op, @Nullable Long remoteId, @Nullable String shareCode, @Nullable Activity activity, @NonNull GeneralCallback<StarredCustomDecksUpdateResponse> callback) {
        if (remoteId == null && shareCode == null)
            throw new IllegalArgumentException();

        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("patch", new JSONObject()
                    .put("type", op.name())
                    .put("remoteId", remoteId)
                    .put("shareCode", shareCode));

            JSONObject obj = api.makePostRequest("Sync/UpdateStarredCustomDecks", body);
            return new StarredCustomDecksUpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, true);
        });
    }

    public void updateStarredCustomDecks(long revision, @NonNull JSONArray update, @Nullable Activity activity, @NonNull GeneralCallback<StarredCustomDecksUpdateResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, true, false);

            JSONObject body = new JSONObject();
            body.put("rev", revision);
            body.put("update", update);

            JSONObject obj = api.makePostRequest("Sync/UpdateStarredCustomDecks", body);
            return new StarredCustomDecksUpdateResponse(obj);
        }), activity, result -> {
            Prefs.putLong(OverloadedPK.STARRED_CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
            callback.onResult(result);
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, false);
        }, ex -> {
            callback.onFailed(ex);
            dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, true);
        });
    }

    public enum SyncProduct {
        STARRED_CARDS, CUSTOM_DECKS, STARRED_CUSTOM_DECKS
    }

    public enum StarredCustomDecksPatchOp {
        ADD, REM
    }

    public enum StarredCardsPatchOp {
        ADD, REM
    }

    public enum CustomDecksPatchOp {
        ADD_CARD, ADD_CARDS, REM_DECK, REM_CARD, ADD_DECK, EDIT_DECK, EDIT_CARD
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

    public static class CustomDecksUpdateResponse {
        public final long[] cardsIds;
        public final Long deckId;
        public final Long cardId;

        private CustomDecksUpdateResponse(@NonNull JSONObject obj) throws JSONException {
            deckId = CommonUtils.optLong(obj, "deckId");
            cardId = CommonUtils.optLong(obj, "cardId");

            JSONArray cardsIdsArray = obj.optJSONArray("cardsIds");
            if (cardsIdsArray == null) {
                cardsIds = null;
            } else {
                cardsIds = new long[cardsIdsArray.length()];
                for (int i = 0; i < cardsIdsArray.length(); i++)
                    cardsIds[i] = cardsIdsArray.getLong(i);
            }
        }
    }

    public static class StarredCardsUpdateResponse {
        public final Long remoteId;
        public final long[] remoteIds;
        public final JSONArray leftover;

        public StarredCardsUpdateResponse(@NonNull JSONObject obj) throws JSONException {
            remoteId = CommonUtils.optLong(obj, "remoteId");
            leftover = obj.optJSONArray("leftover");

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

    public static class CustomDecksSyncResponse {
        public final long remoteId;
        public final boolean needsUpdate;
        public final JSONObject update;
        public final boolean isNew;

        private CustomDecksSyncResponse(@NonNull JSONObject obj) throws JSONException {
            this.remoteId = obj.getLong("id");
            this.isNew = obj.getBoolean("new");
            this.needsUpdate = obj.getBoolean("needsUpdate");
            this.update = obj.optJSONObject("update");
        }

        private static boolean needsSomeUpdates(@NonNull List<CustomDecksSyncResponse> list) {
            for (CustomDecksSyncResponse deck : list)
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

    public static class StarredCustomDecksSyncResponse {
        public final boolean needsUpdate;
        public final JSONArray update;
        public final Long revision;

        private StarredCustomDecksSyncResponse(@NonNull JSONObject resp) throws JSONException {
            needsUpdate = resp.getBoolean("needsUpdate");
            update = resp.optJSONArray("update");
            revision = CommonUtils.optLong(resp, "revision");
        }
    }

    public static class StarredCustomDecksUpdateResponse {
        public final Long remoteId;
        public final long[] remoteIds;
        public final JSONArray leftover;

        public StarredCustomDecksUpdateResponse(@NonNull JSONObject obj) throws JSONException {
            remoteId = CommonUtils.optLong(obj, "remoteId");
            leftover = obj.optJSONArray("leftover");

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
}
