package com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.gianlu.commonutils.Lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.Lifecycle.LifecycleAwareRunnable;
import com.gianlu.pretendyourexyzzy.NetIO.StatusCodeException;
import com.gianlu.pretendyourexyzzy.NetIO.UserAgentInterceptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private final LifecycleAwareHandler handler;

    private UrbanDictApi() {
        executorService = Executors.newSingleThreadExecutor();
        client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
        handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
    }

    @NonNull
    public static UrbanDictApi get() {
        if (instance == null) instance = new UrbanDictApi();
        return instance;
    }

    public final void define(@NonNull String word, @Nullable Activity activity, @NonNull OnDefine listener) {
        executorService.execute(new DefineRunnable(word, activity, listener));
    }

    public final void autocomplete(@NonNull String word, @Nullable Activity activity, @NonNull OnAutoComplete listener) {
        executorService.execute(new AutocompleteRunnable(word, activity, listener));
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

    private class AutocompleteRunnable extends LifecycleAwareRunnable {
        private final String word;
        private final OnAutoComplete listener;

        AutocompleteRunnable(@NonNull String word, @Nullable Activity activity, @NonNull OnAutoComplete listener) {
            super(handler, activity == null ? listener : activity);
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

                    AutoCompleteResults result = new AutoCompleteResults(new JSONObject(json).getJSONArray("results"));
                    post(() -> listener.onResult(result));
                } else {
                    throw new StatusCodeException(resp);
                }
            } catch (IOException | JSONException ex) {
                post(() -> listener.onException(ex));
            }
        }
    }

    private class DefineRunnable extends LifecycleAwareRunnable {
        private final String word;
        private final OnDefine listener;

        DefineRunnable(@NonNull String word, @Nullable Activity activity, @NonNull OnDefine listener) {
            super(handler, activity == null ? listener : activity);
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

                    Definitions result = new Definitions(new JSONObject(json));
                    post(() -> listener.onResult(result));
                } else {
                    throw new StatusCodeException(resp);
                }
            } catch (IOException | JSONException ex) {
                post(() -> listener.onException(ex));
            }
        }
    }
}
