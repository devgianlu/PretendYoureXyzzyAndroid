package com.gianlu.pretendyourexyzzy.api;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.NamedThreadFactory;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.commonutils.ui.OfflineActivity;
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
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
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
    protected final static int AJAX_TIMEOUT = 10;
    protected final static int POLLING_TIMEOUT = 30;
    private static final String TAG = Pyx.class.getSimpleName();
    public final Server server;
    protected final OkHttpClient client;
    protected final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("pyx-"));

    Pyx() throws NoServersException {
        this.server = Server.lastServer();
        this.client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
    }

    protected Pyx(Server server, OkHttpClient client) {
        this.server = server;
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

    @NonNull
    @WorkerThread
    protected final PyxResponse request(@NonNull Op operation, @NonNull PyxRequest.Param... params) throws IOException, JSONException, PyxException {
        Trace trace = FirebasePerformance.startTrace("pyx_request");
        trace.putAttribute("host", server.url.host());
        trace.putAttribute("op", operation.val);
        trace.putAttribute("params_num", String.valueOf(params.length));

        try {
            List<Exception> exceptions = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                try {
                    PyxResponse resp = requestInternal(operation, params);
                    resp.exceptions = exceptions;
                    return resp;
                } catch (SocketTimeoutException ex) {
                    exceptions.add(ex);
                    trace.incrementMetric("tries", 1);
                    Log.d(TAG, "Socket timeout, retrying.", ex);
                } catch (PyxException ex) {
                    exceptions.add(ex);
                    ex.exceptions = exceptions;

                    trace.incrementMetric("pyx_tries", 1);
                    trace.putAttribute("pyx_retry_error", ex.errorCode);
                    if (!ex.shouldRetry())
                        throw ex;
                }
            }

            Exception lastEx = exceptions.get(exceptions.size() - 1);
            if (lastEx instanceof IOException) throw (IOException) lastEx;
            else throw (PyxException) lastEx;
        } finally {
            trace.stop();
        }
    }

    @NonNull
    @WorkerThread
    private PyxResponse requestInternal(@NonNull Op operation, @NonNull PyxRequest.Param... params) throws IOException, JSONException, PyxException {
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
                    Log.d(TAG, "op = " + operation + ", params = " + Arrays.toString(params) + ", code = " + ex.errorCode, ex);
                    throw ex;
                }

                return new PyxResponse(resp, obj);
            } else {
                throw new StatusCodeException(resp);
            }
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof SSLException) throw (SSLException) ex.getCause();
            else throw ex;
        }
    }

    /**
     * Makes a request to the server. This request should have no result.
     *
     * @param request The {@link PyxRequest}
     * @return A task resulting to nothing
     */
    @NonNull
    public final Task<Void> request(@NonNull PyxRequest request) {
        return Tasks.call(executor, () -> {
            request(request.op, request.params);
            return null;
        });
    }

    @NonNull
    public final <E> Task<E> request(@NonNull PyxRequestWithResult<E> request) {
        return Tasks.call(executor, () -> {
            PyxResponse resp = request(request.op, request.params);
            return request.processor.process(resp.resp, resp.obj);
        });
    }

    @NonNull
    @WorkerThread
    public final <E> E requestSync(@NonNull PyxRequestWithResult<E> request) throws JSONException, PyxException, IOException {
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

    @NonNull
    public final Task<UserHistory> getUserHistory(@NonNull String userId) {
        HttpUrl url = server.userHistory(userId);
        if (url == null) return Tasks.forException(new MetricsNotSupportedException(server));

        return Tasks.call(executor, () -> new UserHistory(new JSONObject(requestSync(url))));
    }

    @NonNull
    public final Task<SessionHistory> getSessionHistory(@NonNull String sessionId) {
        HttpUrl url = server.sessionHistory(sessionId);
        if (url == null) return Tasks.forException(new MetricsNotSupportedException(server));

        return Tasks.call(executor, () -> new SessionHistory(new JSONObject(requestSync(url))));
    }

    @NonNull
    public final Task<SessionStats> getSessionStats(@NonNull String sessionId) {
        HttpUrl url = server.sessionStats(sessionId);
        if (url == null) return Tasks.forException(new MetricsNotSupportedException(server));

        return Tasks.call(executor, () -> new SessionStats(new JSONObject(requestSync(url))));
    }

    @NonNull
    public final Task<GameHistory> getGameHistory(@NonNull String gameId) {
        HttpUrl url = server.gameHistory(gameId);
        if (url == null) return Tasks.forException(new MetricsNotSupportedException(server));

        return Tasks.call(executor, () -> new GameHistory(new JSONArray(requestSync(url))));
    }

    @NonNull
    public final Task<GameRound> getGameRound(@NonNull String roundId) {
        HttpUrl url = server.gameRound(roundId);
        if (url == null) return Tasks.forException(new MetricsNotSupportedException(server));

        return Tasks.call(executor, () -> new GameRound(new JSONObject(requestSync(url))));
    }

    @NonNull
    public final Task<FirstLoadedPyx> doFirstLoad() {
        InstanceHolder holder = InstanceHolder.holder();

        try {
            FirstLoadedPyx pyx = holder.get(InstanceHolder.Level.FIRST_LOADED);
            return Tasks.forResult(pyx);
        } catch (LevelMismatchException exx) {
            return Tasks.call(executor, () -> {
                FirstLoadAndConfig result = firstLoadSync();
                FirstLoadedPyx pyx = new FirstLoadedPyx(server, client, result);
                holder.set(pyx);
                return pyx;
            });
        }
    }

    @NonNull
    public Task<File> recoverCardcastDeck(@NonNull String code, @NonNull Context context) {
        return Tasks.call(executor, new Callable<File>() {

            @NonNull
            private JSONObject convertCardObj(@NonNull JSONObject raw) throws JSONException {
                String text = raw.getString("Text");
                return new JSONObject().put("text", CommonUtils.toJSONArray(text.split("____", -1)));
            }

            @Override
            public File call() throws Exception {
                try (Response resp = client.newCall(new Request.Builder().url("https://pretendyoure.xyz/zy/metrics/deck/" + code).build()).execute()) {
                    if (resp.code() != 200)
                        throw new StatusCodeException(resp);

                    ResponseBody body = resp.body();
                    if (body == null)
                        throw new IllegalArgumentException("No response body.");

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

                    return tmpFile;
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
        LIST_CR_CAST_CARD_SETS("clc"),
        ADD_CR_CAST_CARD_SET("cac"),
        REMOVE_CR_CAST_CARD_SET("crc"),
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
    public interface OnEventListener {
        void onPollMessage(@NonNull PollMessage message) throws JSONException;
    }

    public interface OnPollingPyxErrorListener {
        void onPollPyxError(@NonNull PyxException ex);
    }

    public static class NoServersException extends Exception {

        public void solve(@NonNull Context context) {
            OfflineActivity.startActivity(context, null);
        }
    }

    public static class MetricsNotSupportedException extends Exception {
        MetricsNotSupportedException(@NonNull Server server) {
            super("Metrics aren't supported on this server: " + server.name);
        }
    }

    protected static class PyxResponse {
        private final Response resp;
        private final JSONObject obj;
        protected List<Exception> exceptions = null;

        PyxResponse(@NonNull Response resp, @NonNull JSONObject obj) {
            this.resp = resp;
            this.obj = obj;
        }
    }

    public static class Server {
        private static final String TAG = Server.class.getSimpleName();
        private static List<Server> allServers = null;
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

        Server(@NonNull JSONObject obj) throws JSONException {
            this(parseUrlOrThrow(obj.getString("uri")), parseNullableUrl(obj.optString("metrics")), obj.getString("name"),
                    obj.has("params") ? new Params(obj.getJSONObject("params")) : Params.defaultValues(),
                    obj.optBoolean("editable", true));
        }

        @Nullable
        private static HttpUrl parseNullableUrl(@Nullable String url) {
            if (url == null || url.isEmpty()) return null;
            else return HttpUrl.parse(url);
        }

        static void parseAndSave(@NonNull JSONArray array, boolean cache) throws JSONException {
            allServers = null;

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
            if (cache) Prefs.putLong(PK.API_SERVERS_CACHE_AGE, System.currentTimeMillis());
            else Prefs.putLong(PK.API_SERVERS_CACHE_AGE, 0);
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
        public static List<Server> loadCustomServers() {
            return loadServers(PK.USER_SERVERS);
        }

        @NonNull
        public static List<Server> loadAllServers() {
            if (allServers != null && !allServers.isEmpty())
                return allServers;

            List<Server> all = new ArrayList<>(10);
            all.addAll(loadServers(PK.USER_SERVERS));
            all.addAll(loadServers(PK.API_SERVERS));
            return allServers = Collections.unmodifiableList(all);
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

        @Nullable
        public static Server lastServerNoThrow() {
            try {
                return lastServer();
            } catch (Exception ex) {
                return null;
            }
        }

        @NonNull
        public static Server pickBestServer(@NonNull List<Server> servers) {
            if (servers.isEmpty()) throw new IllegalArgumentException();

            for (Server server : servers)
                if (server.url.host().contains("pretendyoure.xyz")) // These servers are usually more populated
                    return server;

            return servers.get(0);
        }

        @NonNull
        public static Server lastServer() throws NoServersException {
            String name = Prefs.getString(PK.LAST_SERVER, null);

            List<Server> apiServers = loadServers(PK.API_SERVERS);
            if (name == null) {
                if (!apiServers.isEmpty()) return pickBestServer(apiServers);
                throw new NoServersException();
            }

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

            if (server == null) {
                if (!apiServers.isEmpty()) return pickBestServer(apiServers);
                throw new NoServersException();
            }

            return server;
        }

        public static void addUserServer(@NonNull Server server) throws JSONException {
            if (!server.isEditable()) return;

            allServers = null;

            JSONArray array = JsonStoring.intoPrefs().getJsonArray(PK.USER_SERVERS);
            if (array == null) array = new JSONArray();
            for (int i = array.length() - 1; i >= 0; i--) {
                if (Objects.equals(array.getJSONObject(i).getString("name"), server.name))
                    array.remove(i);
            }

            array.put(server.toJson());
            JsonStoring.intoPrefs().putJsonArray(PK.USER_SERVERS, array);
        }

        public static void removeUserServer(@NonNull Server server) {
            if (!server.isEditable()) return;

            allServers = null;

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

        public static boolean hasServer(@NonNull String name) {
            try {
                return getServer(PK.USER_SERVERS, name) != null || getServer(PK.API_SERVERS, name) != null;
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing JSON.", ex);
                return true;
            }
        }

        @Nullable
        public static Server fromOverloadedId(@NonNull String serverId) {
            loadAllServers();

            for (Server server : allServers)
                if (OverloadedUtils.getServerId(server).equals(serverId))
                    return server;

            return null;
        }

        public static void setLastServer(@NonNull Server server) {
            Pyx.invalidate();
            Prefs.putString(PK.LAST_SERVER, server.name);

            Log.d(TAG, "Set last server to " + server.url);
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
}
