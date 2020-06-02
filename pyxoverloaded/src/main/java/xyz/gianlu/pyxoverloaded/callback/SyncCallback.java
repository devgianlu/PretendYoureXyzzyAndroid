package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

@UiThread
public interface SyncCallback {
    void onResult(@NonNull OverloadedSyncApi.SyncResponse result);

    void onFailed(@NonNull Exception ex);
}
