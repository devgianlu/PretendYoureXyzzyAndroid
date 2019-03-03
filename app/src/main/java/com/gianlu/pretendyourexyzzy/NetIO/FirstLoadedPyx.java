package com.gianlu.pretendyourexyzzy.NetIO;

import android.app.Activity;

import com.gianlu.commonutils.Lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.Lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CahConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.PK;

import org.json.JSONException;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;

public class FirstLoadedPyx extends Pyx {
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
                        RegisteredPyx pyx = upgrade(user);
                        post(() -> listener.onDone(pyx));
                    } catch (JSONException | PyxException | IOException ex) {
                        post(() -> listener.onException(ex));
                    }
                }
            });
        }
    }

    @NonNull
    public RegisteredPyx upgrade(@NonNull User user) {
        RegisteredPyx pyx = new RegisteredPyx(server, handler, client, firstLoadAndConfig, user);
        InstanceHolder.holder().set(pyx);
        return pyx;
    }
}
