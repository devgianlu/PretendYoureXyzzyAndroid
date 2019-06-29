package com.gianlu.pretendyourexyzzy.NetIO;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.Lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.Lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Deck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.PK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;

public class RegisteredPyx extends FirstLoadedPyx {
    private final User user;
    private final PollingThread pollingThread;

    RegisteredPyx(Server server, LifecycleAwareHandler handler, OkHttpClient client, FirstLoadAndConfig firstLoad, User user) {
        super(server, handler, client, firstLoad);
        this.user = user;
        this.pollingThread = new PollingThread();
        this.pollingThread.start();

        Prefs.putString(PK.LAST_JSESSIONID, user.sessionId);
    }

    @NonNull
    public static RegisteredPyx get() throws LevelMismatchException {
        return InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED);
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
                if (pollingThread != null) pollingThread.safeStop();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Logging.log(ex);
            }
        });

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

    public final void addCardcastDecksAndList(int gid, @NonNull List<String> codes, @NonNull Cardcast cardcast, @Nullable Activity activity, @NonNull OnResult<List<Deck>> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    List<String> failed = new ArrayList<>();
                    for (String code : codes) {
                        try {
                            requestSync(PyxRequests.addCardcastDeck(gid, code));
                        } catch (JSONException | PyxException | IOException ex) {
                            Logging.log(ex);
                            failed.add(code);
                        }
                    }

                    if (!failed.isEmpty()) {
                        post(() -> listener.onException(new PartialCardcastAddFail(failed)));
                    }

                    List<Deck> sets = requestSync(PyxRequests.listCardcastDecks(gid, cardcast));
                    post(() -> listener.onDone(sets));
                } catch (JSONException | PyxException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public final void addCardcastDeckAndList(int gid, @NonNull String code, @NonNull Cardcast cardcast, @Nullable Activity activity, @NonNull OnResult<List<Deck>> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    requestSync(PyxRequests.addCardcastDeck(gid, code));
                    final List<Deck> sets = requestSync(PyxRequests.listCardcastDecks(gid, cardcast));
                    post(() -> listener.onDone(sets));
                } catch (JSONException | PyxException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public static class PartialCardcastAddFail extends Exception {
        private final List<String> codes;

        PartialCardcastAddFail(List<String> codes) {
            this.codes = codes;
        }

        @NonNull
        public String getCodes() {
            return codes.toString();
        }
    }

    public class PollingThread extends Thread {
        private final List<OnEventListener> listeners = new ArrayList<>();
        private final AtomicInteger exCount = new AtomicInteger(0);
        private volatile boolean shouldStop = false;

        @Override
        public void run() {
            while (!shouldStop) {
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

                        if (json.startsWith("{")) {
                            raiseException(new JSONObject(json));
                        } else if (json.startsWith("[")) {
                            dispatchDone(PollMessage.list(new JSONArray(json)));
                        }
                    }
                } catch (IOException | JSONException | PyxException ex) {
                    dispatchEx(ex);
                } catch (IllegalArgumentException ex) {
                    Logging.log(String.format("IAE! {server: %s}", server.url), ex);
                }
            }
        }

        private void dispatchDone(List<PollMessage> messages) {
            exCount.set(0);
            handler.post(null, new NotifyMessage(messages));
        }

        private void dispatchEx(@NonNull Exception ex) {
            exCount.getAndIncrement();
            if (exCount.get() > 5) {
                safeStop();
                handler.post(null, new NotifyException());
            }

            Logging.log(ex);
        }

        public void addListener(@NonNull OnEventListener listener) {
            synchronized (listeners) {
                if (!listeners.contains(listener))
                    listeners.add(listener);
            }
        }

        void safeStop() {
            shouldStop = true;
        }

        public void removeListener(@NonNull OnEventListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        private class NotifyException implements Runnable {

            @Override
            public void run() {
                List<OnEventListener> copy;
                synchronized (listeners) {
                    copy = new ArrayList<>(listeners);
                }

                for (OnEventListener listener : copy)
                    listener.onStoppedPolling();
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
                            dispatchEx(ex);
                        }
                    }
                }
            }
        }
    }
}
