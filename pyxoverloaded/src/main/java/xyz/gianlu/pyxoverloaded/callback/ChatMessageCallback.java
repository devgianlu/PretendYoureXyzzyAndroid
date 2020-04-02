package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;

@UiThread
public interface ChatMessageCallback {
    void onMessage(@NonNull PlainChatMessage msg);

    void onFailed(@NotNull Exception ex);
}
