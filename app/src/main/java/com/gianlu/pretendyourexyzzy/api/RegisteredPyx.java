package com.gianlu.pretendyourexyzzy.api;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.api.models.GameCards;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.api.models.User;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class RegisteredPyx extends FirstLoadedPyx {
    private static final String TAG = RegisteredPyx.class.getSimpleName();
    private final User user;
    private final PollingThread pollingThread;
    private final PyxChatHelper chatHelper;

    RegisteredPyx(Server server, LifecycleAwareHandler handler, OkHttpClient client, FirstLoadAndConfig firstLoad, User user) {
        super(server, handler, client, firstLoad);
        this.user = user;
        this.chatHelper = new PyxChatHelper(handler);
        this.pollingThread = new PollingThread();
        this.pollingThread.start();

        Prefs.putString(PK.LAST_JSESSIONID, user.sessionId);
        AnalyticsApplication.setCrashlyticsString("server", server.url.toString());
    }

    @NonNull
    public static RegisteredPyx get() throws LevelMismatchException {
        return InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED);
    }

    @NonNull
    public PyxChatHelper chat() {
        return chatHelper;
    }

    @Override
    protected final void prepareRequest(@NonNull Op operation, @NonNull Request.Builder request) {
        request.addHeader("Cookie", "JSESSIONID=" + user.sessionId);
    }

    public final void getUserHistory(@Nullable Activity activity, @NonNull OnResult<UserHistory> listener) {
        getUserHistory(user.persistentId, activity, listener);
    }

    @NonNull
    public User user() {
        return user;
    }

    @NonNull
    public PollingThread polling() {
        return pollingThread;
    }

    public final void logout() {
        request(PyxRequests.logout(), null, new OnSuccess() {
            @Override
            public void onDone() {
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed logging out.", ex);
            }
        });

        if (pollingThread != null) pollingThread.safeStop();
        if (OverloadedUtils.isSignedIn()) OverloadedApi.get().loggedOutFromPyxServer();
        InstanceHolder.holder().invalidate();
        Prefs.remove(PK.LAST_JSESSIONID);
    }

    public final void getGameInfoAndCards(int gid, @Nullable Activity activity, @NonNull OnResult<GameInfoAndCards> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    GameInfo info = requestSync(PyxRequests.getGameInfo(gid));
                    GameCards cards = requestSync(PyxRequests.getGameCards(gid));
                    GameInfoAndCards result = new GameInfoAndCards(info, cards);
                    post(() -> listener.onDone(result));
                } catch (JSONException | PyxException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    @Override
    public void close() {
        if (pollingThread != null) pollingThread.safeStop();
        super.close();
    }

    public class PollingThread extends Thread {
        private final List<OnEventListener> listeners = new ArrayList<>();
        private int exCount = 0;
        private volatile boolean shouldStop = false;

        @Override
        public void run() {
            int nextWait = 0;

            while (!shouldStop) {
                if (nextWait > 0) {
                    try {
                        Thread.sleep(nextWait);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }

                try {
                    Request.Builder builder = new Request.Builder()
                            .post(Util.EMPTY_REQUEST)
                            .url(server.polling());

                    builder.header("Cookie", "JSESSIONID=" + user.sessionId);

                    try (Response resp = client.newBuilder()
                            .connectTimeout(POLLING_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(POLLING_TIMEOUT, TimeUnit.SECONDS)
                            .build().newCall(builder.build()).execute()) {
                        if (resp.code() != 200) throw new StatusCodeException(resp);

                        String json;
                        ResponseBody body = resp.body();
                        if (body != null) json = body.string();
                        else throw new IOException("Body is empty!");

                        nextWait = 0;
                        exCount = 0;

                        if (json.startsWith("{")) {
                            raiseException(new JSONObject(json));
                        } else if (json.startsWith("[")) {
                            handler.post(null, new NotifyMessage(PollMessage.list(new JSONArray(json))));
                        }
                    }
                } catch (JSONException | PyxException ex) {
                    Log.w(TAG, "Polling exception.", ex);
                } catch (IOException ex) {
                    Log.w(TAG, "Polling IO exception.", ex);
                    if (exCount++ > 3) {
                        if (nextWait < 500) nextWait = 500;
                        else nextWait *= 2;
                    }
                } catch (IllegalArgumentException ex) {
                    Log.w(TAG, String.format("IAE! {server: %s}", server.url), ex);
                    Bundle bundle = new Bundle();
                    bundle.putString("server", server.url.toString());
                    bundle.putString("msg", ex.getMessage());
                    ThisApplication.sendAnalytics(Utils.ACTION_UNKNOWN_EVENT, bundle);
                }
            }
        }

        public void addListener(@NonNull OnEventListener listener) {
            synchronized (listeners) {
                if (!listeners.contains(listener))
                    listeners.add(listener);
            }
        }

        void safeStop() {
            shouldStop = true;
            interrupt();
        }

        public void removeListener(@NonNull OnEventListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        private class NotifyMessage implements Runnable {
            private final List<PollMessage> messages;

            NotifyMessage(List<PollMessage> messages) {
                this.messages = messages;
            }

            @Override
            public void run() {
                List<OnEventListener> copy;
                synchronized (listeners) {
                    copy = new ArrayList<>(listeners);
                }

                for (OnEventListener listener : copy) {
                    for (PollMessage message : messages) {
                        try {
                            listener.onPollMessage(message);
                        } catch (JSONException ex) {
                            Log.e(TAG, "Failed handling poll message.", ex);
                        }
                    }
                }

                // We need to make sure that the UI has been updated
                executor.execute(() -> {
                    for (PollMessage msg : messages)
                        if (msg.event == PollMessage.Event.CHAT)
                            chatHelper.handleChatEvent(msg);
                });
            }
        }
    }
}
