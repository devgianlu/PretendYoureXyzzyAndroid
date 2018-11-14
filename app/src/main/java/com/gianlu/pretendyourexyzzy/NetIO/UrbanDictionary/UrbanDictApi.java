package com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary;

import android.os.Handler;
import android.os.Looper;

import com.gianlu.pretendyourexyzzy.NetIO.StatusCodeException;
import com.gianlu.pretendyourexyzzy.NetIO.UserAgentInterceptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UrbanDictApi {
    private static final HttpUrl DEFINITION_URL;
    private static final HttpUrl AUTOCOMPLETE_URL;
    private static UrbanDictApi instance;

    static {
        AUTOCOMPLETE_URL = HttpUrl.parse("https://api.urbandictionary.com/v0/autocomplete-extra");
        DEFINITION_URL = HttpUrl.parse("https://api.urbandictionary.com/v0/define");
    }

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

    public final void autocomplete(@NonNull String word, @NonNull OnAutoComplete listener) {
        executorService.execute(new AutocompleteRunnable(word, listener));
    }

    public interface OnDefine {
        @UiThread
        void onResult(@NonNull Definitions result);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    public interface OnAutoComplete {
        @UiThread
        void onResult(@NonNull AutoCompleteResults result);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    private class AutocompleteRunnable implements Runnable {
        private final String word;
        private final OnAutoComplete listener;

        AutocompleteRunnable(@NonNull String word, @NonNull OnAutoComplete listener) {
            this.word = word;
            this.listener = listener;
        }

        @Override
        public void run() {
            Request req = new Request.Builder().get()
                    .url(AUTOCOMPLETE_URL.newBuilder().addQueryParameter("term", word).build())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 200) {
                    ResponseBody body = resp.body();
                    if (body == null) throw new IOException("Body is null!");
                    String json = body.string();
                    final AutoCompleteResults result = new AutoCompleteResults(new JSONObject(json).getJSONArray("results"));
                    handler.post(() -> listener.onResult(result));
                } else {
                    throw new StatusCodeException(resp);
                }
            } catch (IOException | JSONException ex) {
                handler.post(() -> listener.onException(ex));
            }
        }
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
                    .url(DEFINITION_URL.newBuilder().addQueryParameter("term", word).build())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 200) {
                    ResponseBody body = resp.body();
                    if (body == null) throw new IOException("Body is null!");
                    String json = body.string();
                    final Definitions result = new Definitions(new JSONObject(json));
                    handler.post(() -> listener.onResult(result));
                } else {
                    throw new StatusCodeException(resp);
                }
            } catch (IOException | JSONException ex) {
                handler.post(() -> listener.onException(ex));
            }
        }
    }
}
