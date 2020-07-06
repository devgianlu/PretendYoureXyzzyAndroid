package com.gianlu.pretendyourexyzzy.api;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.commonutils.ui.OfflineActivity;
import com.gianlu.pretendyourexyzzy.LoadingActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.CahConfig;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoadAndConfig;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameRound;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionStats;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class Pyx implements Closeable {
    protected final static int AJAX_TIMEOUT = 5;
    protected final static int POLLING_TIMEOUT = 30;
    private static final String TAG = Pyx.class.getSimpleName();
    public final Server server;
    protected final LifecycleAwareHandler handler;
    protected final OkHttpClient client;
    protected final ExecutorService executor = Executors.newFixedThreadPool(5);

    Pyx() throws NoServersException {
        this.handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
        this.server = Server.lastServer();
        this.client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
    }

    protected Pyx(Server server, LifecycleAwareHandler handler, OkHttpClient client) {
        this.server = server;
        this.handler = handler;
        this.client = client;
    }

    @NonNull
    static Pyx getStandard() throws NoServersException {
        return InstanceHolder.holder().instantiateStandard();
    }

    @NonNull
    public static Pyx get() throws LevelMismatchException {
        return InstanceHolder.holder().get(InstanceHolder.Level.STANDARD);
    }

    static void raiseException(@NonNull JSONObject obj) throws PyxException {
        if (obj.optBoolean("e", false) || obj.has("ec")) throw new PyxException(obj);
    }

    public static void invalidate() {
        InstanceHolder.holder().invalidate();
    }

    protected void prepareRequest(@NonNull Op operation, @NonNull Request.Builder request) {
        if (operation == Op.FIRST_LOAD) {
            String lastSessionId = Prefs.getString(PK.LAST_JSESSIONID, null);
            if (lastSessionId != null) request.addHeader("Cookie", "JSESSIONID=" + lastSessionId);
        }
    }

    @WorkerThread
    protected final PyxResponse request(@NonNull Op operation, PyxRequest.Param... params) throws IOException, JSONException, PyxException {
        return request(operation, false, params);
    }

    @NonNull
    @WorkerThread
    private PyxResponse request(@NonNull Op operation, boolean retried, PyxRequest.Param... params) throws IOException, JSONException, PyxException {
        FormBody.Builder reqBody = new FormBody.Builder(StandardCharsets.UTF_8).add("o", operation.val);
        for (PyxRequest.Param pair : params) {
            if (pair.value() != null)
                reqBody.add(pair.key(), pair.value(""));
        }

        Request.Builder builder = new Request.Builder()
                .url(server.ajax())
                .post(reqBody.build());

        prepareRequest(operation, builder);

        try (Response resp = client.newBuilder()
                .connectTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .build().newCall(builder.build()).execute()) {

            ResponseBody respBody = resp.body();
            if (respBody != null) {
                JSONObject obj = new JSONObject(respBody.string());

                try {
                    raiseException(obj);
                    Log.v(TAG, operation + "; " + Arrays.toString(params));
                } catch (PyxException ex) {
                    Log.d(TAG, "op = " + operation + ", params = " + Arrays.toString(params) + ", code = " + ex.errorCode + ", retried = " + retried, ex);
                    if (!retried && ex.shouldRetry()) return request(operation, true, params);
                    throw ex;
                }

                return new PyxResponse(resp, obj);
            } else {
                throw new StatusCodeException(resp);
            }
        } catch (SocketTimeoutException ex) {
            if (!retried) return request(operation, true, params);
            else throw ex;
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof SSLException) throw (SSLException) ex.getCause();
            else throw ex;
        }
    }

    public final void request(@NonNull PyxRequest request, @Nullable Activity activity, @NonNull OnSuccess listener) {
        executor.execute(new RequestRunner(request, activity, listener));
    }

    @WorkerThread
    public final void requestSync(@NonNull PyxRequest request) throws JSONException, PyxException, IOException {
        request(request.op, request.params);
    }

    public final <E> void request(@NonNull PyxRequestWithResult<E> request, @Nullable Activity activity, @NonNull OnResult<E> listener) {
        executor.execute(new RequestWithResultRunner<>(request, activity, listener));
    }

    @NonNull
    @WorkerThread
    public final <E> E requestSync(PyxRequestWithResult<E> request) throws JSONException, PyxException, IOException {
        PyxResponse resp = request(request.op, request.params);
        return request.processor.process(resp.resp, resp.obj);
    }

    @WorkerThread
    @NonNull
    private FirstLoadAndConfig firstLoadSync() throws PyxException, IOException, JSONException {
        FirstLoad fl = requestSync(PyxRequests.firstLoad());

        CahConfig cahConfig;
        try (Response resp = client.newBuilder()
                .connectTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .build().newCall(new Request.Builder()
                        .url(server.config()).get().build()).execute()) {

            ResponseBody respBody = resp.body();
            if (respBody != null) cahConfig = new CahConfig(respBody.string());
            else throw new StatusCodeException(resp);
        }

        try (Response resp = client.newBuilder()
                .connectTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .build().newCall(new Request.Builder()
                        .url(server.stats()).get().build()).execute()) {

            ResponseBody respBody = resp.body();
            if (respBody != null) cahConfig.appendStats(respBody.string());
            else throw new StatusCodeException(resp);
        }

        return new FirstLoadAndConfig(fl, cahConfig);
    }

    @NonNull
    private String requestSync(@NonNull HttpUrl url) throws IOException {
        try (Response resp = client.newBuilder()
                .connectTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .build().newCall(new Request.Builder()
                        .url(url).get().build()).execute()) {

            if (resp.code() < 200 || resp.code() >= 300)
                throw new StatusCodeException(resp);

            ResponseBody respBody = resp.body();
            if (respBody != null) return respBody.string();
            else throw new StatusCodeException(resp);
        }
    }

    public final void getUserHistory(@NonNull String userId, @Nullable Activity activity, @NonNull OnResult<UserHistory> listener) {
        final HttpUrl url = server.userHistory(userId);
        if (url == null) {
            listener.onException(new MetricsNotSupportedException(server));
            return;
        }

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    UserHistory history = new UserHistory(new JSONObject(requestSync(url)));
                    post(() -> listener.onDone(history));
                } catch (JSONException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public final void getSessionHistory(@NonNull String sessionId, @Nullable Activity activity, @NonNull OnResult<SessionHistory> listener) {
        final HttpUrl url = server.sessionHistory(sessionId);
        if (url == null) {
            listener.onException(new MetricsNotSupportedException(server));
            return;
        }

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    SessionHistory history = new SessionHistory(new JSONObject(requestSync(url)));
                    post(() -> listener.onDone(history));
                } catch (JSONException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public final void getSessionStats(@NonNull String sessionId, @Nullable Activity activity, @NonNull OnResult<SessionStats> listener) {
        final HttpUrl url = server.sessionStats(sessionId);
        if (url == null) {
            listener.onException(new MetricsNotSupportedException(server));
            return;
        }

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    SessionStats history = new SessionStats(new JSONObject(requestSync(url)));
                    post(() -> listener.onDone(history));
                } catch (JSONException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public final void getGameHistory(@NonNull String gameId, @Nullable Activity activity, @NonNull OnResult<GameHistory> listener) {
        final HttpUrl url = server.gameHistory(gameId);
        if (url == null) {
            listener.onException(new MetricsNotSupportedException(server));
            return;
        }

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    GameHistory history = new GameHistory(new JSONArray(requestSync(url)));
                    post(() -> listener.onDone(history));
                } catch (JSONException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public final void getGameRound(@NonNull String roundId, @Nullable Activity activity, @NonNull OnResult<GameRound> listener) {
        final HttpUrl url = server.gameRound(roundId);
        if (url == null) {
            listener.onException(new MetricsNotSupportedException(server));
            return;
        }

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    GameRound history = new GameRound(new JSONObject(requestSync(url)));
                    post(() -> listener.onDone(history));
                } catch (JSONException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public final void firstLoad(@Nullable Activity activity, @NonNull OnResult<FirstLoadedPyx> listener) {
        final InstanceHolder holder = InstanceHolder.holder();

        try {
            FirstLoadedPyx pyx = holder.get(InstanceHolder.Level.FIRST_LOADED);
            handler.post(activity == null ? listener : activity, () -> listener.onDone(pyx));
        } catch (LevelMismatchException exx) {
            executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
                @Override
                public void run() {
                    try {
                        FirstLoadAndConfig result = firstLoadSync();

                        FirstLoadedPyx pyx = new FirstLoadedPyx(server, handler, client, result);
                        holder.set(pyx);

                        post(() -> listener.onDone(pyx));
                    } catch (PyxException | IOException | JSONException ex) {
                        post(() -> listener.onException(ex));
                    }
                }
            });
        }
    }

    public void recoverCardcastDeck(@NonNull String code, @NonNull Context context, @NonNull OnRecoverResult callback) {
        executor.execute(new LifecycleAwareRunnable(handler, callback) {

            @NonNull
            private JSONObject convertCardObj(@NonNull JSONObject raw) throws JSONException {
                String text = raw.getString("Text");
                return new JSONObject().put("text", CommonUtils.toJSONArray(text.split("____", -1)));
            }

            @Override
            public void run() {
                try (Response resp = client.newCall(new Request.Builder().url("https://pretendyoure.xyz/zy/metrics/deck/" + code).build()).execute()) {
                    if (resp.code() == 404) {
                        post(callback::notFound);
                        return;
                    } else if (resp.code() != 200) {
                        post(() -> callback.onException(new StatusCodeException(resp)));
                        return;
                    }

                    ResponseBody body = resp.body();
                    if (body == null) {
                        post(() -> callback.onException(new Exception("No response body.")));
                        return;
                    }

                    JSONObject rawObj = new JSONObject(body.string());

                    JSONArray whites = new JSONArray();
                    JSONArray rawWhites = rawObj.getJSONArray("WhiteCards");
                    for (int i = 0; i < rawWhites.length(); i++)
                        whites.put(convertCardObj(rawWhites.getJSONObject(i)));

                    JSONArray blacks = new JSONArray();
                    JSONArray rawBlacks = rawObj.getJSONArray("BlackCards");
                    for (int i = 0; i < rawBlacks.length(); i++)
                        blacks.put(convertCardObj(rawBlacks.getJSONObject(i)));

                    JSONObject obj = new JSONObject();
                    obj.put("calls", blacks).put("responses", whites);
                    obj.put("name", rawObj.getString("Name")).put("watermark", code).put("description", "");

                    File tmpFile = new File(context.getCacheDir(), CommonUtils.randomString(6, "abcdefghijklmnopqrstuvwxyz"));
                    try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                        out.write(obj.toString().getBytes());
                    }

                    post(() -> callback.onDone(tmpFile));
                } catch (IOException | JSONException ex) {
                    post(() -> callback.onException(ex));
                }
            }
        });
    }

    @Override
    public void close() {
        client.dispatcher().executorService().shutdown();
    }

    public boolean hasMetrics() {
        return server.metricsUrl != null;
    }

    public boolean isServerSecure() {
        return server.isHttps();
    }

    public enum Op {
        REGISTER("r"),
        FIRST_LOAD("fl"),
        LOGOUT("lo"),
        GET_GAMES_LIST("ggl"),
        CHAT("c"),
        GET_NAMES_LIST("gn"),
        JOIN_GAME("jg"),
        SPECTATE_GAME("vg"),
        LEAVE_GAME("lg"),
        GET_GAME_INFO("ggi"),
        GET_GAME_CARDS("gc"),
        GAME_CHAT("GC"),
        PLAY_CARD("pc"),
        JUDGE_SELECT("js"),
        CREATE_GAME("cg"),
        START_GAME("sg"),
        CHANGE_GAME_OPTIONS("cgo"),
        LIST_CUSTOM_CARD_SETS("lcs"),
        ADD_CUSTOM_CARD_SET("acs"),
        REMOVE_CUSTOM_CARD_SET("rcs"),
        WHOIS("Wi");

        private final String val;

        Op(@NonNull String val) {
            this.val = val;
        }
    }

    public interface Processor<E> {
        @NonNull
        @WorkerThread
        E process(@NonNull Response response, @NonNull JSONObject obj) throws JSONException;
    }

    @UiThread
    public interface OnSuccess {
        void onDone();

        void onException(@NonNull Exception ex);
    }

    public interface OnRecoverResult {
        void onDone(@NonNull File tmpFile);

        void notFound();

        void onException(@NonNull Exception ex);
    }

    @UiThread
    public interface OnResult<E> {
        void onDone(@NonNull E result);

        void onException(@NonNull Exception ex);
    }

    @UiThread
    public interface OnEventListener {
        void onPollMessage(@NonNull PollMessage message) throws JSONException;
    }

    public static class NoServersException extends Exception {

        public void solve(@NonNull Context context) {
            OfflineActivity.startActivity(context, LoadingActivity.class);
        }
    }

    public static class MetricsNotSupportedException extends Exception {
        MetricsNotSupportedException(Server server) {
            super("Metrics aren't supported on this server: " + server.name);
        }
    }

    protected static class PyxResponse {
        private final Response resp;
        private final JSONObject obj;

        PyxResponse(@NonNull Response resp, @NonNull JSONObject obj) {
            this.resp = resp;
            this.obj = obj;
        }
    }

    public static class Server {
        private static final String TAG = Server.class.getSimpleName();
        public final HttpUrl url;
        public final String name;
        private final boolean editable;
        private final HttpUrl metricsUrl;
        private final Params params;
        public transient volatile ServersChecker.CheckResult status = null;
        private transient HttpUrl ajaxUrl;
        private transient HttpUrl pollingUrl;
        private transient HttpUrl configUrl;
        private transient HttpUrl statsUrl;

        public Server(@NonNull HttpUrl url, @Nullable HttpUrl metricsUrl, @NonNull String name, @NonNull Params params, boolean editable) {
            this.url = url;
            this.metricsUrl = metricsUrl;
            this.name = name;
            this.params = params;
            this.editable = editable;
        }

        Server(JSONObject obj) throws JSONException {
            this(parseUrlOrThrow(obj.getString("uri")), parseNullableUrl(obj.optString("metrics")), obj.getString("name"),
                    obj.has("params") ? new Params(obj.getJSONObject("params")) : Params.defaultValues(),
                    obj.optBoolean("editable", true));
        }

        @Nullable
        private static HttpUrl parseNullableUrl(@Nullable String url) {
            if (url == null || url.isEmpty()) return null;
            else return HttpUrl.parse(url);
        }

        static void parseAndSave(@NonNull JSONArray array) throws JSONException {
            List<Server> servers = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = CommonUtils.getStupidString(obj, "name");

                HttpUrl url = new HttpUrl.Builder()
                        .host(obj.getString("host"))
                        .scheme(obj.getBoolean("secure") ? "https" : "http")
                        .port(obj.getInt("port"))
                        .encodedPath(obj.getString("path"))
                        .build();

                String metrics = CommonUtils.getStupidString(obj, "metrics");
                servers.add(new Server(url, metrics == null ? null : HttpUrl.parse(metrics),
                        name == null ? (url.host() + " server") : name,
                        obj.has("params") ? new Params(obj.getJSONObject("params")) : Params.defaultValues(),
                        false));
            }

            JSONArray json = new JSONArray();
            for (Server server : servers)
                json.put(server.toJson());

            JsonStoring.intoPrefs().putJsonArray(PK.API_SERVERS, json);
            Prefs.putLong(PK.API_SERVERS_CACHE_AGE, System.currentTimeMillis());
        }

        @Nullable
        public static Server fromUrl(Uri url) {
            List<Server> servers = loadAllServers();
            for (Server server : servers) {
                if (server.url.host().equals(url.getHost())
                        && server.url.port() == url.getPort())
                    return server;
            }

            return null;
        }

        @NonNull
        private static HttpUrl parseUrlOrThrow(String str) throws JSONException {
            if (str == null) throw new JSONException("str is null");

            try {
                return HttpUrl.get(str);
            } catch (IllegalArgumentException ex) {
                if (Build.VERSION.SDK_INT >= 27) throw new JSONException(ex);
                else throw new JSONException(ex.getMessage());
            }
        }

        @Nullable
        public static HttpUrl parseUrl(String str) {
            if (str == null) return null;

            try {
                return HttpUrl.parse(str);
            } catch (IllegalStateException ex) {
                Log.w(TAG, "Failed parsing URL: " + str, ex);
                return null;
            }
        }

        @NonNull
        public static List<Server> loadAllServers() {
            List<Server> all = new ArrayList<>(10);
            all.addAll(loadServers(PK.USER_SERVERS));
            all.addAll(loadServers(PK.API_SERVERS));
            return all;
        }

        @NonNull
        private static List<Server> loadServers(Prefs.Key key) {
            List<Server> servers = new ArrayList<>();
            JSONArray array;
            try {
                array = JsonStoring.intoPrefs().getJsonArray(key);
                if (array == null) array = new JSONArray();
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing JSON.", ex);
                return new ArrayList<>();
            }

            for (int i = 0; i < array.length(); i++) {
                try {
                    servers.add(new Server(array.getJSONObject(i)));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed parsing JSON.", ex);
                }
            }

            return servers;
        }

        @Nullable
        private static Server getServer(@NonNull Prefs.Key key, @NonNull String name) throws JSONException {
            JSONArray array = JsonStoring.intoPrefs().getJsonArray(key);
            if (array == null) array = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (Objects.equals(obj.optString("name"), name))
                    return new Server(obj);
            }

            return null;
        }

        @NonNull
        public static Server lastServer() throws NoServersException {
            String name = Prefs.getString(PK.LAST_SERVER, null);

            List<Server> apiServers = loadServers(PK.API_SERVERS);
            if (name == null && !apiServers.isEmpty()) return apiServers.get(0);

            if (name == null) throw new IllegalStateException("Cannot load any server!");

            Server server = null;
            try {
                server = getServer(PK.USER_SERVERS, name);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing JSON.", ex);
            }

            if (server == null) {
                try {
                    server = getServer(PK.API_SERVERS, name);
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed parsing JSON.", ex);
                }
            }

            if (server == null && !apiServers.isEmpty()) server = apiServers.get(0);
            if (server == null) throw new NoServersException();
            return server;
        }

        public static void addUserServer(Server server) throws JSONException {
            if (!server.isEditable()) return;

            JSONArray array = JsonStoring.intoPrefs().getJsonArray(PK.USER_SERVERS);
            if (array == null) array = new JSONArray();
            for (int i = array.length() - 1; i >= 0; i--) {
                if (Objects.equals(array.getJSONObject(i).getString("name"), server.name))
                    array.remove(i);
            }

            array.put(server.toJson());
            JsonStoring.intoPrefs().putJsonArray(PK.USER_SERVERS, array);
        }

        public static void removeUserServer(Server server) {
            if (!server.isEditable()) return;

            try {
                JSONArray array = JsonStoring.intoPrefs().getJsonArray(PK.USER_SERVERS);
                if (array == null) array = new JSONArray();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    if (Objects.equals(obj.optString("name"), server.name)) {
                        array.remove(i);
                        break;
                    }
                }

                JsonStoring.intoPrefs().putJsonArray(PK.USER_SERVERS, array);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing JSON.", ex);
            }
        }

        public static boolean hasServer(String name) {
            try {
                return getServer(PK.USER_SERVERS, name) != null || getServer(PK.API_SERVERS, name) != null;
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing JSON.", ex);
                return true;
            }
        }

        @NonNull
        public Params params() {
            return params;
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Server server = (Server) o;
            return url.equals(server.url) && name.equals(server.name);
        }

        @NonNull
        private JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("params", params == null ? null : params.toJson())
                    .put("name", name)
                    .put("metrics", metricsUrl)
                    .put("editable", editable)
                    .put("uri", url.toString());
        }

        @Nullable
        HttpUrl gameHistory(String id) {
            if (metricsUrl == null) return null;
            return metricsUrl.newBuilder().addPathSegments("game/" + id).build();
        }

        @Nullable
        HttpUrl gameRound(String id) {
            if (metricsUrl == null) return null;
            return metricsUrl.newBuilder().addPathSegments("round/" + id).build();
        }

        @Nullable
        HttpUrl sessionHistory(String id) {
            if (metricsUrl == null) return null;
            return metricsUrl.newBuilder().addPathSegments("session/" + id).build();
        }

        @Nullable
        HttpUrl sessionStats(String id) {
            if (metricsUrl == null) return null;
            return metricsUrl.newBuilder().addPathSegments("session/" + id + "/stats").build();
        }

        @Nullable
        HttpUrl userHistory(String id) {
            if (metricsUrl == null) return null;
            return metricsUrl.newBuilder().addPathSegments("user/" + id).build();
        }

        public boolean isEditable() {
            return editable;
        }

        @NonNull
        HttpUrl stats() {
            if (statsUrl == null)
                statsUrl = url.newBuilder().addPathSegment("stats.jsp").build();

            return statsUrl;
        }

        @NonNull
        HttpUrl ajax() {
            if (ajaxUrl == null)
                ajaxUrl = url.newBuilder().addPathSegment("AjaxServlet").build();

            return ajaxUrl;
        }

        @NonNull
        HttpUrl config() {
            if (configUrl == null)
                configUrl = url.newBuilder().addPathSegments("js/cah.config.js").build();

            return configUrl;
        }

        @NonNull
        public HttpUrl polling() {
            if (pollingUrl == null)
                pollingUrl = url.newBuilder().addPathSegment("LongPollServlet").build();

            return pollingUrl;
        }

        public boolean hasMetrics() {
            return metricsUrl != null;
        }

        public boolean isHttps() {
            return url.isHttps();
        }

        public static class Params {
            public final int blankCardsMin;
            public final int blankCardsMax;
            public final int playersMin;
            public final int playersMax;
            public final int spectatorsMin;
            public final int spectatorsMax;
            public final int scoreMin;
            public final int scoreMax;

            private Params(int blankCardsMin, int blankCardsMax, int playersMin, int playersMax,
                           int spectatorsMin, int spectatorsMax, int scoreMin, int scoreMax) {
                this.blankCardsMin = blankCardsMin;
                this.blankCardsMax = blankCardsMax;
                this.playersMin = playersMin;
                this.playersMax = playersMax;
                this.spectatorsMin = spectatorsMin;
                this.spectatorsMax = spectatorsMax;
                this.scoreMin = scoreMin;
                this.scoreMax = scoreMax;
            }

            private Params(@NonNull JSONObject obj) throws JSONException {
                blankCardsMin = obj.getInt("bl-min");
                blankCardsMax = obj.getInt("bl-max");
                scoreMin = obj.getInt("sl-min");
                scoreMax = obj.getInt("sl-max");
                spectatorsMin = obj.getInt("vL-min");
                spectatorsMax = obj.getInt("vL-max");
                playersMin = obj.getInt("pL-min");
                playersMax = obj.getInt("pL-max");
            }

            @NonNull
            public static Params defaultValues() {
                return new Params(0, 30, 3, 20, 0, 20, 4, 69);
            }

            @NotNull
            JSONObject toJson() throws JSONException {
                JSONObject obj = new JSONObject();
                obj.put("bl-min", blankCardsMin);
                obj.put("bl-max", blankCardsMax);
                obj.put("sl-min", scoreMin);
                obj.put("sl-max", scoreMax);
                obj.put("vL-min", spectatorsMin);
                obj.put("vL-max", spectatorsMax);
                obj.put("pL-min", playersMin);
                obj.put("pL-max", playersMax);
                return obj;
            }
        }
    }

    private class RequestRunner extends LifecycleAwareRunnable {
        private final PyxRequest request;
        private final OnSuccess listener;

        RequestRunner(PyxRequest request, @Nullable Activity activity, @NonNull OnSuccess listener) {
            super(handler, activity == null ? listener : activity);
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                request(request.op, request.params);
                post(listener::onDone);
            } catch (IOException | JSONException | PyxException ex) {
                post(() -> listener.onException(ex));
            }
        }
    }

    private class RequestWithResultRunner<E> extends LifecycleAwareRunnable {
        private final PyxRequestWithResult<E> request;
        private final OnResult<E> listener;

        RequestWithResultRunner(PyxRequestWithResult<E> request, @Nullable Activity activity, @NonNull OnResult<E> listener) {
            super(handler, activity == null ? listener : activity);
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                PyxResponse resp = request(request.op, request.params);
                E result = request.processor.process(resp.resp, resp.obj);
                post(() -> listener.onDone(result));
            } catch (IOException | JSONException | PyxException ex) {
                post(() -> listener.onException(ex));
            }
        }
    }
}
