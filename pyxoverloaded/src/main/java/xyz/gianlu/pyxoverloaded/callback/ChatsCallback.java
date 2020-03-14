package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.List;

import xyz.gianlu.pyxoverloaded.model.Chat;

@UiThread
public interface ChatsCallback {
    void onRemoteChats(@NonNull List<Chat> chats);

    void onLocalChats(@NonNull List<Chat> chats);

    void onFailed(@NonNull Exception ex);
}
