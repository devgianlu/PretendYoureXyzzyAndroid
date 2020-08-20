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

    //region Listeners
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
    //endregion

    //region Starred cards
    public void syncStarredCards(long revision, @NonNull GeneralCallback<StarredCardsSyncResponse> callback) {
        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

                JSONObject obj = api.makePostRequest("Sync/StarredCards", singletonJsonObject("rev", revision));
                StarredCardsSyncResponse resp = new StarredCardsSyncResponse(obj);
                Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, resp.needsUpdate, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);

                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
            }
        });
    }

    public void updateStarredCards(long revision, @NonNull JSONArray update, @NonNull GeneralCallback<StarredCardsUpdateResponse> callback) {
        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

                JSONObject body = new JSONObject();
                body.put("rev", revision);
                body.put("update", update);

                JSONObject obj = api.makePostRequest("Sync/UpdateStarredCards", body);
                StarredCardsUpdateResponse resp = new StarredCardsUpdateResponse(obj);
                Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);

                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
            }
        });
    }

    @Contract("_, _, null, null, _ -> fail")
    public void patchStarredCards(long revision, @NonNull StarredCardsPatchOp op, @Nullable Long remoteId, @Nullable JSONObject item, @NonNull GeneralCallback<StarredCardsUpdateResponse> callback) {
        if (remoteId == null && item == null)
            throw new IllegalArgumentException();

        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, true, false);

                JSONObject body = new JSONObject();
                body.put("rev", revision);
                body.put("patch", new JSONObject()
                        .put("type", op.name())
                        .put("remoteId", remoteId)
                        .put("item", item));

                JSONObject obj = api.makePostRequest("Sync/UpdateStarredCards", body);
                StarredCardsUpdateResponse resp = new StarredCardsUpdateResponse(obj);
                Prefs.putLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);

                dispatchSyncUpdate(SyncProduct.STARRED_CARDS, false, true);
            }
        });
    }
    //endregion

    //region Custom decks
    public void syncCustomDecks(@NonNull JSONArray syncItems, @NonNull GeneralCallback<List<CustomDecksSyncResponse>> callback) {
        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

                JSONObject body = new JSONObject();
                body.put("items", syncItems);

                JSONObject obj = api.makePostRequest("Sync/CustomDecks", body);
                JSONArray items = obj.getJSONArray("items");
                List<CustomDecksSyncResponse> list = new ArrayList<>(items.length());
                for (int i = 0; i < items.length(); i++)
                    list.add(new CustomDecksSyncResponse(items.getJSONObject(i)));

                Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(list);
                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, CustomDecksSyncResponse.needsSomeUpdates(list), false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);
                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
            }
        });
    }

    public void updateCustomDeck(long revision, @NonNull JSONObject update, @NonNull GeneralCallback<CustomDecksUpdateResponse> callback) {
        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

                JSONObject body = new JSONObject();
                body.put("rev", revision);
                body.put("update", update);

                JSONObject obj = api.makePostRequest("Sync/UpdateCustomDecks", body);
                CustomDecksUpdateResponse resp = new CustomDecksUpdateResponse(obj);
                Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);
                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
            }
        });
    }

    public void patchCustomDeck(long revision, @Nullable RemoteId remoteId, @NonNull CustomDecksPatchOp op, @Nullable JSONObject deck, @Nullable JSONObject card,
                                @Nullable RemoteId cardRemoteId, @Nullable JSONArray cards, @NonNull GeneralCallback<CustomDecksUpdateResponse> callback) {
        executorService.execute(() -> {
            try {
                String type;
                Long cardRemoteIdUnwrapped;
                if (op == CustomDecksPatchOp.ADD_EDIT_CARD) {
                    if (cardRemoteId == null) {
                        cardRemoteIdUnwrapped = null;
                        type = "ADD_CARD";
                    } else {
                        try {
                            cardRemoteIdUnwrapped = cardRemoteId.get();
                            type = "EDIT_CARD";
                        } catch (RemoteId.RemoteIdException ignored) {
                            cardRemoteIdUnwrapped = null;
                            type = "ADD_CARD";
                        }
                    }
                } else {
                    cardRemoteIdUnwrapped = cardRemoteId == null ? null : cardRemoteId.get();
                    type = op.name();
                }

                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, true, false);

                JSONObject body = new JSONObject();
                body.put("rev", revision);
                body.put("patch", new JSONObject()
                        .put("id", remoteId != null ? remoteId.get() : null)
                        .put("relId", cardRemoteIdUnwrapped)
                        .put("type", type)
                        .put("deck", deck)
                        .put("cards", cards)
                        .put("card", card));

                JSONObject obj = api.makePostRequest("Sync/UpdateCustomDecks", body);
                CustomDecksUpdateResponse resp = new CustomDecksUpdateResponse(obj);
                Prefs.putLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException | RemoteId.RemoteIdException ex) {
                callback.onFailed(ex);
                dispatchSyncUpdate(SyncProduct.CUSTOM_DECKS, false, true);
            }
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
    //endregion

    //region Custom decks collaborators
    public void getCollaborators(long remoteId, @Nullable Activity activity, @NonNull GeneralCallback<List<String>> callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject obj = api.makePostRequest("Sync/GetCollaborators", singletonJsonObject("remoteId", remoteId));
            return CommonUtils.toStringsList(obj.getJSONArray("collaborators"), false);
        }), activity, callback::onResult, callback::onFailed);
    }

    public void addCollaborator(long remoteId, @NonNull String username, @Nullable Activity activity, @NonNull GeneralCallback<List<String>> callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("remoteId", remoteId).put("username", username);
            JSONObject obj = api.makePostRequest("Sync/AddCollaborator", body);
            return CommonUtils.toStringsList(obj.getJSONArray("collaborators"), false);
        }), activity, callback::onResult, callback::onFailed);
    }

    public void removeCollaborator(long remoteId, @NonNull String username, @Nullable Activity activity, @NonNull GeneralCallback<List<String>> callback) {
        callbacks(Tasks.call(api.executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("remoteId", remoteId).put("username", username);
            JSONObject obj = api.makePostRequest("Sync/RemoveCollaborator", body);
            return CommonUtils.toStringsList(obj.getJSONArray("collaborators"), false);
        }), activity, callback::onResult, callback::onFailed);
    }

    public void patchCollaborator(long revision, @NonNull String shareCode, @NonNull CollaboratorPatchOp op, @Nullable JSONObject card, @Nullable Long cardRemoteId, @Nullable JSONArray cards,
                                  @Nullable Activity activity, @NonNull GeneralCallback<CollaboratorPatchResponse> callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("shareCode", shareCode)
                    .put("rev", revision)
                    .put("patch", new JSONObject()
                            .put("type", op.name())
                            .put("id", cardRemoteId)
                            .put("card", card)
                            .put("cards", cards));

            JSONObject obj = api.makePostRequest("Sync/CollaboratorPatch", body);
            return new CollaboratorPatchResponse(obj);
        }), activity, callback::onResult, callback::onFailed);
    }
    //endregion

    //region Starred custom decks
    public void syncStarredCustomDecks(long revision, @NonNull GeneralCallback<StarredCustomDecksSyncResponse> callback) {
        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, true, false);

                JSONObject obj = api.makePostRequest("Sync/StarredCustomDecks", singletonJsonObject("rev", revision));
                StarredCustomDecksSyncResponse resp = new StarredCustomDecksSyncResponse(obj);
                Prefs.putLong(OverloadedPK.STARRED_CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, resp.needsUpdate, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);
                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, true);
            }
        });
    }

    public void updateStarredCustomDecks(long revision, @NonNull JSONArray update, @NonNull GeneralCallback<StarredCustomDecksUpdateResponse> callback) {
        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, true, false);

                JSONObject body = new JSONObject();
                body.put("rev", revision);
                body.put("update", update);

                JSONObject obj = api.makePostRequest("Sync/UpdateStarredCustomDecks", body);
                StarredCustomDecksUpdateResponse resp = new StarredCustomDecksUpdateResponse(obj);
                Prefs.putLong(OverloadedPK.STARRED_CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException ex) {
                callback.onFailed(ex);
                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, true);
            }
        });
    }

    @Contract("_, _, null, null, _ -> fail")
    public void patchStarredCustomDecks(long revision, @NonNull StarredCustomDecksPatchOp op, @Nullable RemoteId remoteId, @Nullable String shareCode, @NonNull GeneralCallback<StarredCustomDecksUpdateResponse> callback) {
        if (remoteId == null && shareCode == null)
            throw new IllegalArgumentException();

        executorService.execute(() -> {
            try {
                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, true, false);

                JSONObject body = new JSONObject();
                body.put("rev", revision);
                body.put("patch", new JSONObject()
                        .put("type", op.name())
                        .put("remoteId", remoteId != null ? remoteId.get() : null)
                        .put("shareCode", shareCode));

                JSONObject obj = api.makePostRequest("Sync/UpdateStarredCustomDecks", body);
                StarredCustomDecksUpdateResponse resp = new StarredCustomDecksUpdateResponse(obj);
                Prefs.putLong(OverloadedPK.STARRED_CUSTOM_DECKS_LAST_SYNC, OverloadedApi.now());
                callback.onResult(resp);

                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, false);
            } catch (JSONException | OverloadedApi.OverloadedException | OverloadedApi.MaintenanceException | RemoteId.RemoteIdException ex) {
                callback.onFailed(ex);
                dispatchSyncUpdate(SyncProduct.STARRED_CUSTOM_DECKS, false, true);
            }
        });
    }
    //endregion

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
        ADD_EDIT_CARD, ADD_CARDS, REM_DECK, REM_CARD, ADD_DECK, EDIT_DECK
    }

    public enum CollaboratorPatchOp {
        ADD_CARD, EDIT_CARD, ADD_CARDS, REM_CARD
    }

    public interface SyncStatusListener {
        void syncStatusUpdated(@NonNull SyncProduct product, boolean isSyncing, boolean error);
    }

    public abstract static class RemoteId {
        @Nullable
        protected abstract Long getInternal();

        public final long get() throws RemoteIdException {
            Long remoteId = getInternal();
            if (remoteId == null) throw new RemoteIdException();
            else return remoteId;
        }

        public static class RemoteIdException extends Exception {
        }
    }

    private static class SyncStatus {
        final boolean isSyncing;
        final boolean error;

        SyncStatus(boolean isSyncing, boolean error) {
            this.isSyncing = isSyncing;
            this.error = error;
        }
    }

    public static class CollaboratorPatchResponse {
        public final long[] cardsIds;
        public final Long cardId;

        private CollaboratorPatchResponse(@NonNull JSONObject obj) throws JSONException {
            cardId = CommonUtils.optLong(obj, "cardId");

            JSONArray cardsIdsArray = obj.optJSONArray("cardsIds");
            if (cardsIdsArray == null) cardsIds = null;
            else cardsIds = CommonUtils.toLongsList(cardsIdsArray);
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
            if (cardsIdsArray == null) cardsIds = null;
            else cardsIds = CommonUtils.toLongsList(cardsIdsArray);
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
                remoteIds = CommonUtils.toLongsList(obj.getJSONArray("remoteIds"));
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
                remoteIds = CommonUtils.toLongsList(obj.getJSONArray("remoteIds"));
            }
        }
    }
}
