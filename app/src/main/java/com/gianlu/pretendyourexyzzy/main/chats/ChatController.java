package com.gianlu.pretendyourexyzzy.main.chats;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ChatController {
    void init() throws InitException;

    void listener(@NonNull Listener listener);

    void send(@NonNull String msg, @Nullable Activity activity, @NonNull SendCallback callback);

    void onDestroy();

    interface SendCallback {
        void onSuccessful();

        void unknownCommand();

        void onFailed(@NonNull Exception ex);
    }

    interface Listener {
        void onChatMessage(@NonNull ChatMessage msg);
    }

    class InitException extends Exception {
        InitException(Throwable cause) {
            super(cause);
        }
    }

    class ChatMessage {
        public final String sender;
        public final String text;
        public final boolean wall;
        public final boolean emote;
        public final long timestamp;

        ChatMessage(@NonNull String sender, @NonNull String text, boolean wall, boolean emote, long timestamp) {
            this.sender = sender;
            this.text = text;
            this.wall = wall;
            this.emote = emote;
            this.timestamp = timestamp;
        }
    }
}
