package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedPK;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.callback.SyncCallback;
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
                return -1; // FIXME
            default:
                throw new IllegalArgumentException("Unknown product: " + product);
        }
    }

    public static void syncStarredCards(@NonNull Context context) {
        long ourRevision = StarredCardsDatabase.getRevision();
        OverloadedSyncApi.get().syncStarredCards(ourRevision, null, new SyncCallback() {
            @Override
            public void onResult(@NonNull OverloadedSyncApi.SyncResponse result) {
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
