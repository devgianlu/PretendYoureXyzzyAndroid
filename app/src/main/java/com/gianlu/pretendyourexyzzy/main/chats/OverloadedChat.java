package com.gianlu.pretendyourexyzzy.main.chats;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

public class OverloadedChat implements ChatController {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void init() throws InitException {

    }

    @Override
    public void listener(@NonNull Listener listener) {

    }

    @Override
    public void send(@NonNull String msg, @Nullable Activity activity, @NonNull SendCallback callback) {

    }

    @Override
    public void onDestroy() {

    }
}
