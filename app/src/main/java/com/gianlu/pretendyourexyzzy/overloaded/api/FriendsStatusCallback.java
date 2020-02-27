package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

@UiThread
public interface FriendsStatusCallback {
    void onFriendsStatus(@NotNull Map<String, OverloadedApi.FriendStatus> result);

    void onFailed(@NotNull Exception ex);
}
