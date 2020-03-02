package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

@UiThread
public interface ChatMessagesCallback {
    void onMessages(@NonNull OverloadedApi.ChatMessages messages);

    void onFailed(@NotNull Exception ex);
}
