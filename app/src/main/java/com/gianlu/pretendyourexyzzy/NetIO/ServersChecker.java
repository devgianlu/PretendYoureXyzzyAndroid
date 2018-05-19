package com.gianlu.pretendyourexyzzy.NetIO;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.Logging;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ServersChecker {
    private static final int TIMEOUT = 5; // sec
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
        ResponseBody body = response.body();
        if (body == null) return CheckResult.error(new NullPointerException("body is null!"));

        try {
            return CheckResult.online(new CheckResult.Stats(body.string()), latency);
        } catch (ParseException ex) {
            throw new IOException(ex);
        }
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
        void serverChecked(Pyx.Server server);
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
        public static CheckResult offline(@NonNull Throwable ex) {
            return new CheckResult(ServerStatus.OFFLINE, ex, null, -1);
        }

        @NonNull
        public static CheckResult error(@NonNull Throwable ex) {
            return new CheckResult(ServerStatus.ERROR, ex, null, -1);
        }

        @NonNull
        public static CheckResult online(@NonNull Stats stats, long latency) {
            return new CheckResult(ServerStatus.ERROR, null, stats, latency);
        }

        public static class Stats {
            private static final Pattern PATTERN = Pattern.compile("(.+?)\\s(.+)");
            public final int maxUsers;
            public final int users;
            public final int maxGames;
            public final int games;

            Stats(String str) throws ParseException {
                Matcher matcher = PATTERN.matcher(str);

                Integer maxUsersTmp = null;
                Integer usersTmp = null;
                Integer gamesTmp = null;
                Integer maxGamesTmp = null;
                while (matcher.find()) {
                    String name = matcher.group(1);
                    String val = matcher.group(2);

                    switch (name) {
                        case "USERS":
                            usersTmp = Integer.parseInt(val);
                            break;
                        case "GAMES":
                            gamesTmp = Integer.parseInt(val);
                            break;
                        case "MAX_USERS":
                            maxUsersTmp = Integer.parseInt(val);
                            break;
                        case "MAX_GAMES":
                            maxGamesTmp = Integer.parseInt(val);
                            break;
                    }
                }

                if (maxUsersTmp == null) throw new ParseException("MAX_USERS", 0);
                else maxUsers = maxUsersTmp;

                if (usersTmp == null) throw new ParseException("USERS", 0);
                else users = usersTmp;

                if (maxGamesTmp == null) throw new ParseException("MAX_GAMES", 0);
                else maxGames = maxGamesTmp;

                if (gamesTmp == null) throw new ParseException("GAMES", 0);
                else games = gamesTmp;
            }
        }
    }

    public class Runner implements Runnable {
        private final Pyx.Server server;
        private final OnResult listener;

        Runner(Pyx.Server server, OnResult listener) {
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
                Logging.log(ex);
                server.status = handleException(ex);
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.serverChecked(server);
                }
            });
        }
    }
}
