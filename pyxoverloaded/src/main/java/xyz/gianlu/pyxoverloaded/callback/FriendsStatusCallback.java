package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.UiThread;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import xyz.gianlu.pyxoverloaded.model.FriendStatus;

@UiThread
public interface FriendsStatusCallback {
    void onFriendsStatus(@NotNull Map<String, FriendStatus> result);

    void onFailed(@NotNull Exception ex);
}
