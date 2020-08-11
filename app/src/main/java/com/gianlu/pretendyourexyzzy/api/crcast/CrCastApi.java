package com.gianlu.pretendyourexyzzy.api.crcast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.StatusCodeException;
import com.gianlu.pretendyourexyzzy.api.UserAgentInterceptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class CrCastApi {
    private static final CrCastApi instance = new CrCastApi();
    private static final String BASE_URL = "https://castapi.clrtd.com/";
    private final OkHttpClient client;
    private final ExecutorService executorService;
    private final LifecycleAwareHandler handler;

    private CrCastApi() {
        executorService = Executors.newSingleThreadExecutor();
        client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
        handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
    }

    @NonNull
    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat getApiDateTimeFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @NonNull
    public static CrCastApi get() {
        return instance;
    }

    @NonNull
    @WorkerThread
    private JSONObject request(@NonNull String suffix) throws IOException, JSONException {
        try (Response resp = client.newCall(new Request.Builder().get().url(BASE_URL + suffix).build()).execute()) {
            if (resp.code() != 200) throw new StatusCodeException(resp);

            ResponseBody body = resp.body();
            if (body == null) throw new IOException("Missing body.");
            JSONObject json = new JSONObject(body.string());

            int errorCode;
            if ((errorCode = json.optInt("error", 0)) != 0)
                throw new CrCastException(errorCode);

            return json;
        }
    }

    @NonNull
    private String getToken() throws CrCastException {
        String token = Prefs.getString(PK.LAST_CR_CAST_TOKEN, null);
        if (token == null || token.isEmpty()) throw new NotSignedInException();
        return token;
    }

    public void login(@NonNull String username, @NonNull String password, @Nullable Activity activity, LoginCallback callback) {
        executorService.execute(new LifecycleAwareRunnable(handler, activity == null ? callback : activity) {
            @Override
            public void run() {
                String passwordHash;
                try {
                    passwordHash = new String(MessageDigest.getInstance("SHA512").digest(password.getBytes(StandardCharsets.UTF_8)));
                } catch (GeneralSecurityException ex) {
                    post(() -> callback.onException(ex));
                    return;
                }

                try {
                    JSONObject obj = request("user/token/?username=" + username + "&password=" + passwordHash);
                    String token = obj.getString("token"); // TODO: Does this expire?
                    Prefs.putString(PK.LAST_CR_CAST_TOKEN, token);
                    post(callback::onLoginSuccessful);
                } catch (IOException | JSONException ex) {
                    post(() -> callback.onException(ex));
                }
            }
        });
    }

    public void getUser(@Nullable Activity activity, @NonNull UserCallback callback) {
        executorService.execute(new LifecycleAwareRunnable(handler, activity == null ? callback : activity) {
            @Override
            public void run() {
                try {
                    JSONObject obj = request("user/" + getToken());
                    CrCastUser user = new CrCastUser(obj);
                    post(() -> callback.onUser(user));
                } catch (IOException | JSONException | ParseException ex) {
                    post(() -> callback.onException(ex));
                }
            }
        });
    }

    public void getDecks(@Nullable Activity activity, @NonNull DecksCallback callback) {
        executorService.execute(new LifecycleAwareRunnable(handler, activity == null ? callback : activity) {
            @Override
            public void run() {
                try {
                    JSONObject decks = request("user/decks/" + getToken()).getJSONObject("decks");

                    List<CrCastDeck> list = new ArrayList<>(decks.length());
                    Iterator<String> iter = decks.keys();
                    while (iter.hasNext())
                        list.add(new CrCastDeck(decks.getJSONObject(iter.next())));
                    post(() -> callback.onDecks(list));
                } catch (IOException | JSONException | ParseException ex) {
                    post(() -> callback.onException(ex));
                }
            }
        });
    }

    public interface UserCallback {
        void onUser(@NonNull CrCastUser user);

        void onException(@NonNull Exception ex);
    }

    public interface DecksCallback {
        void onDecks(@NonNull List<CrCastDeck> decks);

        void onException(@NonNull Exception ex);
    }

    public interface LoginCallback {
        void onLoginSuccessful();

        void onException(@NonNull Exception ex);
    }

    public static class NotSignedInException extends CrCastException {

        NotSignedInException() {
            super("Missing token.");
        }
    }

    public static class CrCastException extends IOException {
        CrCastException(int errorCode) {
            super("Code: " + errorCode);
        }

        CrCastException(@NonNull String msg) {
            super(msg);
        }
    }
}
