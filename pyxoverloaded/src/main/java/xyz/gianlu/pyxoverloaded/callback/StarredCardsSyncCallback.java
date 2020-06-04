package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

@UiThread
public interface StarredCardsSyncCallback {
    void onResult(@NonNull OverloadedSyncApi.StarredCardsSyncResponse result);

    void onFailed(@NonNull Exception ex);
}
