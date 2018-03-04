package com.gianlu.pretendyourexyzzy.NetIO;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.gianlu.commonutils.Logging;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
    private static Request createRequest(PYX.Server server) {
        return new Request.Builder().get().url(server.url).build();
    }

    private void validateResponse(PYX.Server server, Response response, long latency) { // TODO: Should be more strict
        CheckResult result;
        if (response.code() == 200) result = new CheckResult(ServerStatus.ONLINE, latency);
        else result = new CheckResult(ServerStatus.ERROR, -1);
        server.status = result;
    }

    private void handleException(PYX.Server server, Throwable ex) {
        CheckResult result;
        if (ex instanceof SocketTimeoutException || ex instanceof UnknownHostException)
            result = new CheckResult(ServerStatus.OFFLINE, -1);
        else
            result = new CheckResult(ServerStatus.ERROR, -1);

        server.status = result;
    }

    public void check(PYX.Server server, OnResult listener) {
        server.status = null;
        executorService.execute(new Runner(server, listener));
    }

    public enum ServerStatus {
        ONLINE,
        ERROR,
        OFFLINE
    }

    public interface OnResult {
        void serverChecked(PYX.Server server);
    }

    public class CheckResult {
        public final ServerStatus status;
        public final long latency;

        CheckResult(ServerStatus status, long latency) {
            this.status = status;
            this.latency = latency;
        }
    }

    public class Runner implements Runnable {
        private final PYX.Server server;
        private final OnResult listener;

        Runner(PYX.Server server, OnResult listener) {
            this.server = server;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                Response response = client.newCall(createRequest(server)).execute();
                validateResponse(server, response, System.currentTimeMillis() - start);
            } catch (IOException ex) {
                Logging.log(ex);
                handleException(server, ex);
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
