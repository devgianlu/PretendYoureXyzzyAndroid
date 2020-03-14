package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.model.ChatMessage;

@UiThread
public interface ChatMessageCallback {
    void onMessage(@NonNull ChatMessage msg);

    void onFailed(@NotNull Exception ex);
}
