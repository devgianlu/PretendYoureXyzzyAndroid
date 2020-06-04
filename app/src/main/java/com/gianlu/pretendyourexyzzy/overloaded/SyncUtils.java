package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomCard;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedPK;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CustomDeckSyncResponse;
import xyz.gianlu.pyxoverloaded.callback.CustomDecksSyncCallback;
import xyz.gianlu.pyxoverloaded.callback.StarredCardsSyncCallback;
import xyz.gianlu.pyxoverloaded.callback.UpdateSyncCallback;

public final class SyncUtils {
    private static final String TAG = SyncUtils.class.getSimpleName();

    private SyncUtils() {
    }

    @NonNull
    private static String formatTime(long time) {
        Calendar today = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        SimpleDateFormat sdf;
        if (today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) && today.get(Calendar.YEAR) == cal.get(Calendar.YEAR))
            sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        else
            sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());

        return sdf.format(time);
    }

    public static long getLastSync(@NonNull OverloadedSyncApi.SyncProduct product) {
        switch (product) {
            case STARRED_CARDS:
                return Prefs.getLong(OverloadedPK.STARRED_CARDS_LAST_SYNC, -1);
            case CUSTOM_DECKS:
                return Prefs.getLong(OverloadedPK.CUSTOM_DECKS_LAST_SYNC, -1);
            default:
                throw new IllegalArgumentException("Unknown product: " + product);
        }
    }

    public static void syncStarredCards(@NonNull Context context) {
        long ourRevision = StarredCardsDatabase.getRevision();
        OverloadedSyncApi.get().syncStarredCards(ourRevision, null, new StarredCardsSyncCallback() {
            @Override
            public void onResult(@NonNull OverloadedSyncApi.StarredCardsSyncResponse result) {
                if (result.needsUpdate) {
                    StarredCardsDatabase.UpdatePair update = StarredCardsDatabase.get(context).getUpdate();
                    OverloadedSyncApi.get().updateStarredCards(ourRevision, update.update, null, new UpdateSyncCallback() {
                        @Override
                        public void onResult(@NonNull OverloadedSyncApi.UpdateResponse result) {
                            if (result.remoteIds == null) return;

                            update.setRemoteIds(result.remoteIds);
                            Log.i(TAG, "Updated starred cards on server, count: " + result.remoteIds.length);
                        }

                        @Override
                        public void onFailed(@NonNull Exception ex) {
                            Log.e(TAG, "Failed updating starred cards.", ex);
                        }
                    });
                } else if (result.update != null && result.revision != null) {
                    StarredCardsDatabase.get(context).loadUpdate(result.update, result.revision);
                    Log.i(TAG, String.format("Received starred cards from server, count: %d, revision: %d", result.update.length(), result.revision));
                } else {
                    Log.d(TAG, "Starred cards are up-to-date: " + ourRevision);
                }
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed syncing starred cards.", ex);
            }
        });
    }

    public static void syncCustomDecks(@NonNull Context context) {
        CustomDecksDatabase db = CustomDecksDatabase.get(context);

        int[] decksIds;
        JSONObject revisions = new JSONObject();
        JSONArray local = new JSONArray();
        try {
            List<CustomDeck> decks = db.getDecks();
            decksIds = new int[decks.size()];
            for (int i = 0; i < decks.size(); i++) {
                CustomDeck deck = decks.get(i);
                decksIds[i] = deck.id;

                if (deck.remoteId != null) {
                    revisions.put(String.valueOf(deck.remoteId), deck.revision);
                } else {
                    JSONObject obj = new JSONObject();
                    obj.put("rev", deck.revision);
                    obj.put("deck", new JSONObject().put("name", deck.name).put("desc", deck.description).put("watermark", deck.watermark));
                    obj.put("cards", CustomCard.toSyncJson(db.getCards(deck.id)));
                    local.put(obj);
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating sync payload for custom decks.", ex);
            return;
        }

        OverloadedSyncApi.get().syncCustomDecks(revisions, local, null, new CustomDecksSyncCallback() {
            @Override
            public void onResult(@NonNull List<CustomDeckSyncResponse> result) {
                if (result.size() != decksIds.length) {
                    Log.e(TAG, String.format("IDs number doesn't match, local: %d, remote: %d", decksIds.length, result.size()));
                    return;
                }

                for (int i = 0; i < result.size(); i++)
                    updateCustomDeck(db, decksIds[i], result.get(i));

                Log.d(TAG, String.format("Received %d updates.", result.size()));
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed syncing custom decks.", ex);
            }
        });
    }

    private static void updateCustomDeck(@NonNull CustomDecksDatabase db, int deckId, @NonNull CustomDeckSyncResponse result) {
        if (result.needsUpdate) {
            CustomDeck deck = db.getDeck(deckId);
            if (deck == null) return;

            JSONObject update;
            try {
                update = new JSONObject();
                update.put("deck", deck.toSyncJson());
                update.put("cards", CustomCard.toSyncJson(db.getCards(deck.id)));
            } catch (JSONException ex) {
                Log.e(TAG, "Failed creating deck update.", ex);
                return;
            }

            OverloadedSyncApi.get().updateCustomDeck(deck.revision, update, null, new UpdateSyncCallback() {
                @Override
                public void onResult(@NonNull OverloadedSyncApi.UpdateResponse result) {
                    if (result.remoteId == null) return;

                    db.setRemoteId(deckId, result.remoteId);
                    Log.i(TAG, "Sent deck update successfully: " + deckId);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed sending deck update: " + deckId, ex);
                }
            });
        } else if (result.update != null) {
            db.loadDeckUpdate(result.update);
        } else {
            db.setRemoteId(deckId, result.remoteId);
        }
    }

    public static void updateSyncText(@NonNull TextView view, @NonNull OverloadedSyncApi.SyncProduct product, boolean isSyncing, boolean error) {
        if (OverloadedApi.get().isUnderMaintenance()) {
            view.setText(R.string.overloadedSync_maintenance);
        } else if (!OverloadedApi.get().isFullyRegistered()) {
            view.setText(R.string.overloadedSync_notLoggedIn);
        } else if (isSyncing) {
            view.setText(R.string.overloadedSyncing);
        } else {
            long lastSync = getLastSync(product);
            if (error) {
                if (lastSync == -1)
                    view.setText(R.string.overloadedSync_errorNeverSynced);
                else
                    view.setText(view.getContext().getString(R.string.overloadedSync_errorSynced, formatTime(lastSync)));
            } else {
                if (lastSync == -1)
                    view.setText(R.string.overloadedSync_neverSynced);
                else
                    view.setText(view.getContext().getString(R.string.overloadedSync_synced, formatTime(lastSync)));
            }
        }
    }
}
