package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;

public interface UserDataCallback {
    void onUserData(@NonNull OverloadedApi.UserData data);

    void onFailed(@NonNull Exception ex);
}
