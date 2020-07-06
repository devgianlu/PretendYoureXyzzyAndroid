package com.gianlu.pretendyourexyzzy.api;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.CahConfig;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.api.models.User;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class FirstLoadedPyx extends Pyx {
    private static final String TAG = FirstLoadedPyx.class.getSimpleName();
    private final FirstLoadAndConfig firstLoadAndConfig;

    FirstLoadedPyx(Server server, LifecycleAwareHandler handler, OkHttpClient client, FirstLoadAndConfig firstLoadAndConfig) {
        super(server, handler, client);
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

    public final void register(@NonNull String nickname, @Nullable String idCode, @Nullable Activity activity, @NonNull OnResult<RegisteredPyx> listener) {
        try {
            listener.onDone(InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED));
        } catch (LevelMismatchException exx) {
            executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
                @Override
                public void run() {
                    try {
                        User user = requestSync(PyxRequests.register(nickname, idCode, Prefs.getString(PK.LAST_PERSISTENT_ID, null)));
                        Prefs.putString(PK.LAST_PERSISTENT_ID, user.persistentId);
                        if (OverloadedUtils.isSignedIn()) {
                            try {
                                Tasks.await(OverloadedApi.get().loggedIntoPyxServer(server.url, nickname));
                            } catch (ExecutionException | InterruptedException ex) {
                                Log.d(TAG, "Failed sending logged into pyx.", ex);
                            }
                        }

                        RegisteredPyx pyx = upgrade(user, true);
                        post(() -> listener.onDone(pyx));
                    } catch (JSONException | PyxException | IOException ex) {
                        post(() -> listener.onException(ex));
                    }
                }
            });
        }
    }

    @NonNull
    private RegisteredPyx upgrade(@NonNull User user, boolean internal) {
        RegisteredPyx pyx = new RegisteredPyx(server, handler, client, firstLoadAndConfig, user);
        InstanceHolder.holder().set(pyx);

        if (OverloadedUtils.isSignedIn() && !internal)
            OverloadedApi.get().loggedIntoPyxServer(server.url, user.nickname);

        return pyx;
    }

    public void upgrade(@NonNull User user) {
        upgrade(user, false);
    }
}
