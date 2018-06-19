package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    RegisteredPyx(Server server, Handler handler, OkHttpClient client, SharedPreferences preferences, FirstLoadAndConfig firstLoad, User user) {
        super(server, handler, client, preferences, firstLoad);
        this.user = user;
        this.pollingThread = new PollingThread();
        this.pollingThread.start();

        Prefs.putString(preferences, PK.LAST_JSESSIONID, user.sessionId);
    }

    @NonNull
    public static RegisteredPyx get() throws LevelMismatchException {
        return InstanceHolder.holder().get(InstanceHolder.Level.REGISTERED);
    }

    @Override
    protected final void prepareRequest(@NonNull Op operation, @NonNull Request.Builder request) {
        request.addHeader("Cookie", "JSESSIONID=" + user.sessionId);
    }

    public final void getUserHistory(OnResult<UserHistory> listener) {
        getUserHistory(user.persistentId, listener);
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
        request(PyxRequests.logout(), new OnSuccess() {
            @Override
            public void onDone() {
                if (pollingThread != null) pollingThread.safeStop();
                InstanceHolder.holder().invalidate();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Logging.log(ex);
                InstanceHolder.holder().invalidate();
            }
        });
    }

    public final void getGameInfoAndCards(final int gid, final OnResult<GameInfoAndCards> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GameInfo info = requestSync(PyxRequests.getGameInfo(gid));
                    GameCards cards = requestSync(PyxRequests.getGameCards(gid));
                    final GameInfoAndCards result = new GameInfoAndCards(info, cards);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(result);
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

    public final void addCardcastDecksAndList(final int gid, final List<String> codes, @NonNull final Cardcast cardcast, final OnResult<List<Deck>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<String> failed = new ArrayList<>();

                    for (String code : codes) {
                        try {
                            requestSync(PyxRequests.addCardcastDeck(gid, code));
                        } catch (JSONException | PyxException | IOException ex) {
                            Logging.log(ex);
                            failed.add(code);
                        }
                    }

                    if (!failed.isEmpty()) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onException(new PartialCardcastAddFail(failed));
                            }
                        });
                    }

                    final List<Deck> sets = requestSync(PyxRequests.listCardcastDecks(gid, cardcast));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(sets);
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

    public final void addCardcastDeckAndList(final int gid, @NonNull final String code, @NonNull final Cardcast cardcast, final OnResult<List<Deck>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    requestSync(PyxRequests.addCardcastDeck(gid, code));
                    final List<Deck> sets = requestSync(PyxRequests.listCardcastDecks(gid, cardcast));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(sets);
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
        private final Map<String, OnEventListener> listeners = new HashMap<>();
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
                }
            }
        }

        private void dispatchDone(List<PollMessage> messages) {
            exCount.set(0);
            handler.post(new NotifyMessage(messages));
        }

        private void dispatchEx(Exception ex) {
            exCount.getAndIncrement();
            if (exCount.get() > 5) {
                safeStop();
                handler.post(new NotifyException());
            }

            Logging.log(ex);
        }

        public void addListener(String tag, OnEventListener listener) {
            this.listeners.put(tag, listener);
        }

        void safeStop() {
            shouldStop = true;
        }

        public void removeListener(String tag) {
            this.listeners.remove(tag);
        }

        private class NotifyException implements Runnable {

            @Override
            public void run() {
                for (OnEventListener listener : listeners.values())
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
                for (OnEventListener listener : listeners.values()) {
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
