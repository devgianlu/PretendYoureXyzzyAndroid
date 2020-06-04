package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;

import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

public interface CustomDecksSyncCallback {
    void onResult(@NonNull List<OverloadedSyncApi.CustomDeckSyncResponse> result);

    void onFailed(@NonNull Exception ex);
}
