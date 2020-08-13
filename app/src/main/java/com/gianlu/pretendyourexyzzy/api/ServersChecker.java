package com.gianlu.pretendyourexyzzy.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class ServersChecker {
    private static final int TIMEOUT = 5; // sec
    private static final String TAG = ServersChecker.class.getSimpleName();
    private final OkHttpClient client;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Handler handler;

    public ServersChecker() {
        this.handler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @NonNull
    private static Request createRequest(@NonNull Pyx.Server server) {
        return new Request.Builder().get().url(server.stats()).build();
    }

    @NonNull
    private CheckResult validateResponse(@NonNull Response response, long latency) throws IOException {
        if (response.code() == 404)
            return CheckResult.offline(new IOException("404! Server is down."));
        else if (response.code() == 520)
            return CheckResult.offline(new IOException("Unknown server error, try again later."));

        ResponseBody body = response.body();
        if (body == null) return CheckResult.error(new NullPointerException("body is null!"));

        return CheckResult.online(new CheckResult.Stats(body.string()), latency);
    }

    @NonNull
    private CheckResult handleException(@NonNull Throwable ex) {
        if (ex instanceof SocketTimeoutException || ex instanceof UnknownHostException)
            return CheckResult.offline(ex);
        else
            return CheckResult.error(ex);
    }

    public void check(@NonNull Pyx.Server server, OnResult listener) {
        server.status = null;
        executorService.execute(new Runner(server, listener));
    }

    public enum ServerStatus {
        ONLINE,
        ERROR,
        OFFLINE
    }

    public interface OnResult {
        void serverChecked(@NonNull Pyx.Server server);
    }

    public static class CheckResult {
        public final ServerStatus status;
        public final Stats stats;
        public final long latency;
        public final Throwable ex;

        private CheckResult(@NonNull ServerStatus status, @Nullable Throwable ex, @Nullable Stats stats, long latency) {
            this.status = status;
            this.ex = ex;
            this.stats = stats;
            this.latency = latency;
        }

        @NonNull
        static CheckResult offline(@NonNull Throwable ex) {
            return new CheckResult(ServerStatus.OFFLINE, ex, null, -1);
        }

        @NonNull
        static CheckResult error(@NonNull Throwable ex) {
            return new CheckResult(ServerStatus.ERROR, ex, null, -1);
        }

        @NonNull
        static CheckResult online(@NonNull Stats stats, long latency) {
            return new CheckResult(ServerStatus.ONLINE, null, stats, latency);
        }

        public static class Stats {
            private static final Pattern PATTERN = Pattern.compile("(.+?)\\s(.+)");
            private final Map<String, String> map = new HashMap<>();

            Stats(@NonNull String str) {
                Matcher matcher = PATTERN.matcher(str);

                while (matcher.find()) {
                    map.put(matcher.group(1), matcher.group(2));
                }
            }

            private boolean getOrDefault(String key, boolean def) {
                String val = map.get(key);
                return val == null ? def : Boolean.parseBoolean(val);
            }

            private int getOrDefault(String key, int def) {
                String val = map.get(key);
                return val == null ? def : Integer.parseInt(val);
            }

            public int users() {
                return getOrDefault("USERS", 0);
            }

            public int maxUsers() {
                return getOrDefault("MAX_USERS", 0);
            }

            public int games() {
                return getOrDefault("GAMES", 0);
            }

            public int maxGames() {
                return getOrDefault("MAX_GAMES", 0);
            }

            public boolean globalChatEnabled() {
                return getOrDefault("GLOBAL_CHAT_ENABLED", false);
            }

            public boolean gameChatEnabled() {
                return getOrDefault("GAME_CHAT_ENABLED", false);
            }

            public boolean blankCardsEnabled() {
                return getOrDefault("BLANK_CARDS_ENABLED", false);
            }

            public boolean customDecksEnabled() {
                return getOrDefault("CUSTOM_DECKS_ENABLED", false);
            }

            public boolean crCastEnabled() {
                return getOrDefault("CR_CAST_ENABLED", false);
            }
        }
    }

    public class Runner implements Runnable {
        private final Pyx.Server server;
        private final OnResult listener;

        Runner(@NonNull Pyx.Server server, @NonNull OnResult listener) {
            this.server = server;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                Response response = client.newCall(createRequest(server)).execute();
                server.status = validateResponse(response, System.currentTimeMillis() - start);
            } catch (IOException ex) {
                Log.e(TAG, "Failed checking server.", ex);
                server.status = handleException(ex);
            }

            handler.post(() -> listener.serverChecked(server));
        }
    }
}
