package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

public interface UpdateSyncCallback {
    void onResult(@NonNull OverloadedSyncApi.UpdateResponse result);

    void onFailed(@NonNull Exception ex);
}
