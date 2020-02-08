package com.gianlu.pretendyourexyzzy.main.chats;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.overloaded.OverloadedApi;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OverloadedChat implements ChatController {
    private final List<ListenerRegistration> registrations = new ArrayList<>();
    private OverloadedApi.ChatModule chat;

    @Override
    public void init() throws InitException {
        chat = OverloadedApi.get().chat();
        if (chat == null) throw new InitException("Couldn't initialize Overloaded chat!");
    }

    @Override
    public void listener(@NonNull Listener listener) {
        if (chat == null) throw new IllegalStateException();
        chat.addListener(listener).addOnSuccessListener(registrations::addAll);
    }

    @Override
    public void send(@NonNull String msg, @Nullable Activity activity, @NonNull SendCallback callback) {
        if (chat == null) throw new IllegalStateException();
        chat.send("" /* TODO: Missing chat ID */, msg).addOnCompleteListener(task -> {
            if (task.isSuccessful()) callback.onSuccessful();
            else if (task.getException() != null) callback.onFailed(task.getException());
            else throw new IllegalStateException();
        });
    }

    @Override
    public void onDestroy() {
        Iterator<ListenerRegistration> iter = registrations.iterator();
        while (iter.hasNext()) {
            iter.next().remove();
            iter.remove();
        }
    }
}
