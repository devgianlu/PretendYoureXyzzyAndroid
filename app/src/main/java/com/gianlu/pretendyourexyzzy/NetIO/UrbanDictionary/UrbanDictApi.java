package com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.gianlu.pretendyourexyzzy.NetIO.StatusCodeException;
import com.gianlu.pretendyourexyzzy.NetIO.UserAgentInterceptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UrbanDictApi {
    private static UrbanDictApi instance;
    private final ExecutorService executorService;
    private final OkHttpClient client;
    private final Handler handler;

    private UrbanDictApi() {
        executorService = Executors.newSingleThreadExecutor();
        client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
        handler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public static UrbanDictApi get() {
        if (instance == null) instance = new UrbanDictApi();
        return instance;
    }

    public final void define(@NonNull String word, @NonNull OnDefine listener) {
        executorService.execute(new DefineRunnable(word, listener));
    }

    public interface OnDefine {
        @UiThread
        void onResult(@NonNull Definitions result);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    private class DefineRunnable implements Runnable {
        private final String word;
        private final OnDefine listener;

        DefineRunnable(@NonNull String word, @NonNull OnDefine listener) {
            this.word = word;
            this.listener = listener;
        }

        @Override
        public void run() {
            Request req = new Request.Builder().get()
                    .url("http://api.urbandictionary.com/v0/define?term=" + word.replace(' ', '+'))
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 200) {
                    ResponseBody body = resp.body();
                    if (body == null) throw new IOException("Body is null!");
                    String json = body.string();
                    final Definitions result = new Definitions(new JSONObject(json));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(result);
                        }
                    });
                } else {
                    throw new StatusCodeException(resp);
                }
            } catch (final IOException | JSONException ex) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onException(ex);
                    }
                });
            }
        }
    }
}
