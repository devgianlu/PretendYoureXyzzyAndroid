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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedPK;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CustomDecksSyncResponse;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CustomDecksUpdateResponse;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.StarredCardsSyncResponse;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.StarredCardsUpdateResponse;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;

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
        OverloadedSyncApi.get().syncStarredCards(ourRevision, null, new GeneralCallback<StarredCardsSyncResponse>() {
            @Override
            public void onResult(@NonNull StarredCardsSyncResponse result) {
                if (result.needsUpdate) {
                    StarredCardsDatabase.UpdatePair update = StarredCardsDatabase.get(context).getUpdate();
                    OverloadedSyncApi.get().updateStarredCards(ourRevision, update.update, null, new GeneralCallback<StarredCardsUpdateResponse>() {
                        @Override
                        public void onResult(@NonNull StarredCardsUpdateResponse result) {
                            if (result.remoteIds == null) {
                                Log.e(TAG, "Received invalid response when syncing starred cards.");
                                return;
                            }

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

        List<CustomDeck> sendLater = new LinkedList<>();
        JSONArray syncItems = new JSONArray();
        try {
            List<CustomDeck> decks = db.getDecks();
            for (CustomDeck deck : decks) {
                if (deck.remoteId != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", deck.remoteId);
                    obj.put("rev", deck.revision);
                    syncItems.put(obj);
                } else {
                    sendLater.add(deck);
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating sync payload for custom decks.", ex);
            return;
        }

        Log.d(TAG, String.format("Sending %d sync items for custom decks.", syncItems.length()));
        OverloadedSyncApi.get().syncCustomDecks(syncItems, null, new GeneralCallback<List<CustomDecksSyncResponse>>() {
            @Override
            public void onResult(@NonNull List<CustomDecksSyncResponse> result) {
                for (CustomDecksSyncResponse resp : result) {
                    if (resp.needsUpdate) {
                        CustomDeck deck = db.getDeckByRemoteId(resp.remoteId);
                        if (deck != null) sendCustomDeckUpdate(db, deck);
                    } else if (resp.update != null) {
                        db.loadDeckUpdate(resp.update, resp.isNew);
                        Log.d(TAG, "Loaded update for custom deck: " + resp.remoteId);
                    }
                }

                for (CustomDeck deck : sendLater)
                    sendCustomDeckUpdate(db, deck);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed syncing custom decks.", ex);
            }
        });
    }

    private static void sendCustomDeckUpdate(@NonNull CustomDecksDatabase db, @NonNull CustomDeck deck) {
        int[] cardsIds;
        JSONObject update;
        try {
            update = new JSONObject();
            update.put("deck", deck.toSyncJson());

            List<CustomCard> cards = db.getCards(deck.id);
            update.put("cards", CustomCard.toSyncJson(cards));

            cardsIds = new int[cards.size()];
            for (int i = 0; i < cardsIds.length; i++)
                cardsIds[i] = cards.get(i).id;
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating custom deck update.", ex);
            return;
        }

        Log.d(TAG, "Sending update for custom deck: " + deck);
        OverloadedSyncApi.get().updateCustomDeck(deck.revision, update, null, new GeneralCallback<CustomDecksUpdateResponse>() {
            @Override
            public void onResult(@NonNull CustomDecksUpdateResponse result) {
                if (result.cardsIds == null || result.deckId == null) {
                    Log.e(TAG, "Received invalid response after updating custom deck.");
                    return;
                }

                db.setDeckRemoteId(deck.id, result.deckId);

                if (result.cardsIds.length != cardsIds.length) {
                    Log.e(TAG, String.format("Custom decks cards IDs number doesn't match, local: %d, remote: %d", cardsIds.length, result.cardsIds.length));
                    return;
                }

                for (int i = 0; i < cardsIds.length; i++)
                    db.setCardRemoteId(cardsIds[i], result.cardsIds[i]);

                Log.i(TAG, "Sent custom deck update successfully: " + deck);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                if (ex instanceof OverloadedApi.OverloadedServerException && ((OverloadedApi.OverloadedServerException) ex).httpCode == 403)
                    db.resetRemoteIds(deck.id);

                Log.e(TAG, "Failed sending custom deck update: " + deck, ex);
            }
        });
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
