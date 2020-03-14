package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.List;

import xyz.gianlu.pyxoverloaded.model.Chat;

@UiThread
public interface ChatsCallback {
    void onChats(@NonNull List<Chat> chats);

    void onFailed(@NonNull Exception ex);
}
