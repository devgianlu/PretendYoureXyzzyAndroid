package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;

public interface GeneralCallback<T> {
    void onResult(@NonNull T result);

    void onFailed(@NonNull Exception ex);
}
