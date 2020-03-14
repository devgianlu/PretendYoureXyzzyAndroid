package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import xyz.gianlu.pyxoverloaded.model.Chat;

@UiThread
public interface ChatCallback {
    void onChat(@NonNull Chat chat);

    void onFailed(@NonNull Exception ex);
}
