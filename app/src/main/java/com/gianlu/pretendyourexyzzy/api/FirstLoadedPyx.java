package com.gianlu.pretendyourexyzzy.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.CahConfig;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.api.models.User;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class FirstLoadedPyx extends Pyx {
    private static final String TAG = FirstLoadedPyx.class.getSimpleName();
    private final FirstLoadAndConfig firstLoadAndConfig;

    FirstLoadedPyx(Server server, OkHttpClient client, FirstLoadAndConfig firstLoadAndConfig) {
        super(server, client);
        this.firstLoadAndConfig = firstLoadAndConfig;
    }

    @NonNull
    public FirstLoad firstLoad() {
        return firstLoadAndConfig.firstLoad;
    }

    @NonNull
    public CahConfig config() {
        return firstLoadAndConfig.cahConfig;
    }

    @NonNull
    public final Task<RegisteredPyx> register(@NonNull String nickname, @Nullable String idCode) {
        try {
            return Tasks.forResult(InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED));
        } catch (LevelMismatchException exx) {
            return Tasks.call(executor, () -> {
                User user = requestSync(PyxRequests.register(nickname, idCode, Prefs.getString(PK.LAST_PERSISTENT_ID, null)));
                Prefs.putString(PK.LAST_PERSISTENT_ID, user.persistentId);
                if (OverloadedUtils.isSignedIn()) {
                    try {
                        Tasks.await(OverloadedApi.get().loggedIntoPyxServer(server.url, nickname));
                    } catch (ExecutionException | InterruptedException ex) {
                        Log.d(TAG, "Failed sending logged into pyx.", ex);
                    }
                }

                try {
                    Tasks.await(OverloadedApi.get().listUsersOnServer(server.url));
                } catch (ExecutionException ex) {
                    Log.e(TAG, "Failed getting online users.", ex);
                }

                return user;
            }).continueWithTask(task -> upgrade(task.getResult(), true));
        }
    }

    @NonNull
    private Task<RegisteredPyx> upgrade(@NonNull User user, boolean internal) {
        RegisteredPyx pyx = new RegisteredPyx(server, client, firstLoadAndConfig, user);
        InstanceHolder.holder().set(pyx);

        Task<?> task = null;
        if (OverloadedUtils.isSignedIn() && !internal)
            task = OverloadedApi.get().loggedIntoPyxServer(server.url, user.nickname);

        if (task == null)
            task = OverloadedApi.get().listUsersOnServer(server.url);
        else
            task = task.continueWithTask(task1 -> OverloadedApi.get().listUsersOnServer(server.url));

        return task.continueWithTask(task1 -> {
            if (task1.getException() != null)
                Log.e(TAG, "Failed getting online users (upgrade).", task1.getException());

            return Tasks.forResult(pyx);
        });
    }

    @NotNull
    public Task<RegisteredPyx> upgrade(@NonNull User user) {
        return upgrade(user, false);
    }
}
