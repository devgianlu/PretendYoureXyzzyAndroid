package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.model.ChatMessages;

@UiThread
public interface ChatMessagesCallback {
    void onRemoteMessages(@NonNull ChatMessages messages);

    void onLocalMessages(@NonNull ChatMessages messages);

    void onFailed(@NotNull Exception ex);
}
