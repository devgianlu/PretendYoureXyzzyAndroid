package com.gianlu.pretendyourexyzzy.overloaded.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface FriendsStatusCallback {
    void onFriendsStatus(@NotNull Map<String, OverloadedApi.FriendStatus> result);

    void onFailed(@NotNull Exception ex);
}
