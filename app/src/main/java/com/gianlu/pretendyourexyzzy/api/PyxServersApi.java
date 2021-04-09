package com.gianlu.pretendyourexyzzy.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.NamedThreadFactory;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PyxServersApi {
    private static final HttpUrl GET_ALL_SERVERS_URL = HttpUrl.parse("https://pyx-overloaded.gianlu.xyz/AllServers");
    private static final String TAG = PyxServersApi.class.getSimpleName();
    private static PyxServersApi instance;
    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("pyx-discovery-"));

    private PyxServersApi() {
        this.client = new OkHttpClient();
    }

    @NonNull
    public static PyxServersApi get() {
        if (instance == null) instance = new PyxServersApi();
        return instance;
    }

    @NonNull
    private static JSONArray defaultServers(@NonNull Context context) throws IOException, JSONException {
        return new JSONArray(CommonUtils.readEntirely(context.getResources().openRawResource(R.raw.default_servers)));
    }

    @WorkerThread
    private void loadDiscoveryApiServersSync(@NonNull Context context) throws IOException, JSONException {
        try {
            if (!CommonUtils.isDebug() && Prefs.has(PK.API_SERVERS) && !JsonStoring.intoPrefs().isJsonArrayEmpty(PK.API_SERVERS)) {
                long age = Prefs.getLong(PK.API_SERVERS_CACHE_AGE, 0);
                if (System.currentTimeMillis() - age < TimeUnit.HOURS.toMillis(1))
                    return;
            }

            try (Response resp = client.newCall(new Request.Builder().url(GET_ALL_SERVERS_URL).get().build()).execute()) {
                ResponseBody respBody = resp.body();
                if (respBody != null) {
                    JSONArray array = new JSONArray(respBody.string());
                    if (array.length() > 0) Pyx.Server.parseAndSave(array, true);
                } else {
                    throw new StatusCodeException(resp);
                }
            }
        } catch (IOException | JSONException ex) {
            if (JsonStoring.intoPrefs().isJsonArrayEmpty(PK.API_SERVERS)) {
                Log.e(TAG, "Failed loading servers, loaded default servers.", ex);
                Pyx.Server.parseAndSave(defaultServers(context), false);
            } else {
                Log.e(TAG, "Failed loading servers, but list isn't empty.", ex);
            }
        }
    }

    /**
     * Utility method to call {@link Pyx#doFirstLoad()} after loading the PYX servers.
     *
     * @param context The calling {@link Context}
     * @return A task resulting to {@link FirstLoadedPyx}
     */
    @NonNull
    public Task<FirstLoadedPyx> firstLoad(@NonNull Context context) {
        return Tasks.call(executor, () -> {
            loadDiscoveryApiServersSync(context);
            return null;
        }).continueWithTask(executor, task -> {
            try {
                return Pyx.getStandard().doFirstLoad();
            } catch (Pyx.NoServersException ex) {
                ex.solve(context);
                throw ex;
            }
        });
    }
}
