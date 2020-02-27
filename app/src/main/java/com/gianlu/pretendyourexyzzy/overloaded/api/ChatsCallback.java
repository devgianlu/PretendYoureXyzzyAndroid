package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import java.util.List;

@UiThread
public interface ChatsCallback {
    void onChats(@NonNull List<OverloadedApi.Chat> chats);

    void onFailed(@NonNull Exception ex);
}
