package com.gianlu.pretendyourexyzzy.api;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.misc.NamedThreadFactory;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PyxDiscoveryApi {
    private static final HttpUrl WELCOME_MSG_URL = HttpUrl.parse("https://pyx-discovery.gianlu.xyz/WelcomeMessage");
    private static final HttpUrl DISCOVERY_API_LIST = HttpUrl.parse("https://pyx-discovery.gianlu.xyz/ListAll");
    private static final String TAG = PyxDiscoveryApi.class.getSimpleName();
    private static PyxDiscoveryApi instance;
    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("pyx-discovery-"));
    private final LifecycleAwareHandler handler;

    private PyxDiscoveryApi() {
        this.client = new OkHttpClient();
        this.handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
    }

    @NonNull
    public static PyxDiscoveryApi get() {
        if (instance == null) instance = new PyxDiscoveryApi();
        return instance;
    }

    @NonNull
    private static JSONArray defaultServers(@NonNull Context context) throws IOException, JSONException {
        return new JSONArray(CommonUtils.readEntirely(context.getResources().openRawResource(R.raw.default_servers)));
    }

    private void loadDiscoveryApiServersSync(@NonNull Context context) throws IOException, JSONException {
        try {
            if (!CommonUtils.isDebug() && Prefs.has(PK.API_SERVERS) && !JsonStoring.intoPrefs().isJsonArrayEmpty(PK.API_SERVERS)) {
                long age = Prefs.getLong(PK.API_SERVERS_CACHE_AGE, 0);
                if (System.currentTimeMillis() - age < TimeUnit.HOURS.toMillis(6))
                    return;
            }

            JSONArray array = new JSONArray(requestSync(DISCOVERY_API_LIST));
            if (array.length() > 0) Pyx.Server.parseAndSave(array, true);
        } catch (IOException | JSONException ex) {
            if (JsonStoring.intoPrefs().isJsonArrayEmpty(PK.API_SERVERS)) {
                Log.e(TAG, "Failed loading servers, loaded default servers.", ex);
                Pyx.Server.parseAndSave(defaultServers(context), false);
            } else {
                Log.e(TAG, "Failed loading servers, but list isn't empty.", ex);
            }
        }
    }

    @NonNull
    private String requestSync(@NonNull HttpUrl url) throws IOException {
        try (Response resp = client.newCall(new Request.Builder()
                .url(url).get().build()).execute()) {

            ResponseBody respBody = resp.body();
            if (respBody != null) {
                return respBody.string();
            } else {
                throw new StatusCodeException(resp);
            }
        }
    }

    public final void getWelcomeMessage(@Nullable Activity activity, @NonNull Pyx.OnResult<String> listener) {
        String cached = Prefs.getString(PK.WELCOME_MSG_CACHE, null);
        if (cached != null && !CommonUtils.isDebug()) {
            long age = Prefs.getLong(PK.WELCOME_MSG_CACHE_AGE, 0);
            if (System.currentTimeMillis() - age < TimeUnit.HOURS.toMillis(12)) {
                listener.onDone(cached);
                return;
            }
        }

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(requestSync(WELCOME_MSG_URL));
                    final String msg = obj.getString("msg");
                    Prefs.putString(PK.WELCOME_MSG_CACHE, msg);
                    Prefs.putLong(PK.WELCOME_MSG_CACHE_AGE, System.currentTimeMillis());
                    post(() -> listener.onDone(msg));
                } catch (JSONException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public void firstLoad(@NonNull Context context, @Nullable Activity activity, @NonNull Pyx.OnResult<FirstLoadedPyx> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    loadDiscoveryApiServersSync(context);
                    Pyx.getStandard().firstLoad(activity, listener);
                } catch (IOException | JSONException ex) {
                    post(() -> listener.onException(ex));
                } catch (Pyx.NoServersException ex) {
                    ex.solve(context);
                    post(() -> listener.onException(ex));
                }
            }
        });
    }
}
