package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

@UiThread
public interface ChatMessageCallback {
    void onMessage(@NonNull OverloadedApi.ChatMessage msg);

    void onFailed(@NotNull Exception ex);
}
