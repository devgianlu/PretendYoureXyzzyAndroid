package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.List;

@UiThread
public interface UsersCallback {
    void onUsers(@NonNull List<String> list);

    void onFailed(@NonNull Exception ex);
}
