package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;

import okhttp3.OkHttpClient;

public class FirstLoadedPyx extends Pyx {
    protected final FirstLoad firstLoad;

    FirstLoadedPyx(Server server, Handler handler, OkHttpClient client, SharedPreferences preferences, FirstLoad firstLoad) {
        super(server, handler, client, preferences);
        this.firstLoad = firstLoad;
    }

    @NonNull
    public final FirstLoad firstLoad() {
        return firstLoad;
    }

    public final void register(@NonNull String nickname, final OnResult<RegisteredPyx> listener) {
        try {
            listener.onDone((RegisteredPyx) InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED));
        } catch (LevelMismatchException exx) {
            request(PyxRequests.register(nickname), new OnResult<User>() {
                @Override
                public void onDone(@NonNull User result) {
                    listener.onDone(upgrade(result));
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    listener.onException(ex);
                }
            });
        }
    }

    @NonNull
    public RegisteredPyx upgrade(@NonNull User user) {
        RegisteredPyx pyx = new RegisteredPyx(server, handler, client, preferences, firstLoad, user);
        InstanceHolder.holder().set(pyx);
        return pyx;
    }
}
