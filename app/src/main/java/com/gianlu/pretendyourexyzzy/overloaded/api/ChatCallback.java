package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

@UiThread
public interface ChatCallback {
    void onChat(@NonNull OverloadedApi.Chat chat);

    void onFailed(@NonNull Exception ex);
}
