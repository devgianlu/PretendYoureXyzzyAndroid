package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PyxDiscoveryApi {
    private static final HttpUrl WELCOME_MSG_URL = HttpUrl.parse("https://pyx-discovery.gianlu.xyz/WelcomeMessage");
    private static final HttpUrl DISCOVERY_API_LIST = HttpUrl.parse("https://pyx-discovery.gianlu.xyz/ListAll");
    private static PyxDiscoveryApi instance;
    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler handler;

    private PyxDiscoveryApi() {
        this.client = new OkHttpClient();
        this.handler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public static PyxDiscoveryApi get() {
        if (instance == null) instance = new PyxDiscoveryApi();
        return instance;
    }

    private void loadDiscoveryApiServersSync() throws IOException, JSONException {
        if (!CommonUtils.isDebug() && Prefs.has(PK.API_SERVERS) && !Prefs.isJSONArrayEmpty(PK.API_SERVERS)) {
            long age = Prefs.getLong(PK.API_SERVERS_CACHE_AGE, 0);
            if (System.currentTimeMillis() - age < TimeUnit.HOURS.toMillis(6))
                return;
        }

        JSONArray array = new JSONArray(requestSync(DISCOVERY_API_LIST));
        Pyx.Server.parseAndSave(array);
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

    public final void getWelcomeMessage(final Pyx.OnResult<String> listener) {
        String cached = Prefs.getString(PK.WELCOME_MSG_CACHE, null);
        if (cached != null && !CommonUtils.isDebug()) {
            long age = Prefs.getLong(PK.WELCOME_MSG_CACHE_AGE, 0);
            if (System.currentTimeMillis() - age < TimeUnit.HOURS.toMillis(12)) {
                listener.onDone(cached);
                return;
            }
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(requestSync(WELCOME_MSG_URL));
                    final String msg = obj.getString("msg");
                    Prefs.putString(PK.WELCOME_MSG_CACHE, msg);
                    Prefs.putLong(PK.WELCOME_MSG_CACHE_AGE, System.currentTimeMillis());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(msg);
                        }
                    });
                } catch (JSONException | IOException ex) {
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

    public void firstLoad(@NonNull final Context context, @NonNull final Pyx.OnResult<FirstLoadedPyx> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    loadDiscoveryApiServersSync();
                    Pyx.getStandard().firstLoad(listener);
                } catch (IOException | JSONException ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onException(ex);
                        }
                    });
                } catch (final Pyx.NoServersException ex) {
                    ex.solve(context);
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
}
