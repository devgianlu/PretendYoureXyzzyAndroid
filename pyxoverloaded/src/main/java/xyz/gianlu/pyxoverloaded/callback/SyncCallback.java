package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

@UiThread
public interface SyncCallback {
    void onResult(@NonNull OverloadedApi.SyncResponse result);

    void onFailed(@NonNull Exception ex);
}
