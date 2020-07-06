package xyz.gianlu.pyxoverloaded.callback;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import xyz.gianlu.pyxoverloaded.model.UserData;

@UiThread
public interface UserDataCallback {
    void onUserData(@NonNull UserData data);

    void onFailed(@NonNull Exception ex);
}
