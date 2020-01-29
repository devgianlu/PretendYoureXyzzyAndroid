package com.gianlu.pretendyourexyzzy.main.chats;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.api.overloaded.OverloadedApi;
import com.google.firebase.firestore.ListenerRegistration;

public class OverloadedChat implements ChatController {
    private OverloadedApi.ChatModule chat;
    private volatile ListenerRegistration listenerRegistration;

    @Override
    public void init() throws InitException {
        chat = OverloadedApi.get().chat();
        if (chat == null) throw new InitException("Couldn't initialize Overloaded chat!");
    }

    @Override
    public void listener(@NonNull Listener listener) {
        if (chat == null) throw new IllegalStateException();

        chat.addListener(listener).addOnSuccessListener(lr -> listenerRegistration = lr);
    }

    @Override
    public void send(@NonNull String msg, @Nullable Activity activity, @NonNull SendCallback callback) {
        // TODO
    }

    @Override
    public void onDestroy() {
        if (listenerRegistration != null) listenerRegistration.remove();
    }
}
