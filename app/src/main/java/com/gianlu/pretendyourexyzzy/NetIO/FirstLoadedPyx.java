package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.NetIO.Models.CahConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;

import org.json.JSONException;

import java.io.IOException;

import okhttp3.OkHttpClient;

public class FirstLoadedPyx extends Pyx {
    private final FirstLoadAndConfig firstLoadAndConfig;

    FirstLoadedPyx(Server server, Handler handler, OkHttpClient client, SharedPreferences preferences, FirstLoadAndConfig firstLoadAndConfig) {
        super(server, handler, client, preferences);
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

    public final void register(@NonNull final String nickname, @Nullable final String idCode, final OnResult<RegisteredPyx> listener) {
        try {
            listener.onDone((RegisteredPyx) InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED));
        } catch (LevelMismatchException exx) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        User user = requestSync(PyxRequests.register(nickname, idCode));
                        final RegisteredPyx pyx = upgrade(user);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onDone(pyx);
                            }
                        });
                    } catch (JSONException | PyxException | IOException ex) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onException(ex);
                            }
                        });
                    }
                }
            });
        }
    }

    @NonNull
    public RegisteredPyx upgrade(@NonNull User user) {
        RegisteredPyx pyx = new RegisteredPyx(server, handler, client, preferences, firstLoadAndConfig, user);
        InstanceHolder.holder().set(pyx);
        return pyx;
    }
}
