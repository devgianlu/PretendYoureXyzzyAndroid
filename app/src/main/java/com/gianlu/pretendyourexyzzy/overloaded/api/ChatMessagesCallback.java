package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@UiThread
public interface ChatMessagesCallback {
    void onMessages(@NonNull List<OverloadedApi.ChatMessage> msg);

    void onFailed(@NotNull Exception ex);
}
