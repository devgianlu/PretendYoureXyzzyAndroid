package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

@UiThread
public interface UserDataCallback {
    void onUserData(@NonNull OverloadedApi.UserData data);

    void onFailed(@NonNull Exception ex);
}
