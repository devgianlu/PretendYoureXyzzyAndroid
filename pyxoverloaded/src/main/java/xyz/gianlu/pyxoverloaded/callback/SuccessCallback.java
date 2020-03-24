package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

@UiThread
public interface SuccessCallback {
    void onSuccessful();

    void onFailed(@NonNull Exception ex);
}
