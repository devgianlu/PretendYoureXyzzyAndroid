package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NameValuePair;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.PKeys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class Pyx implements Closeable {
    protected final static int AJAX_TIMEOUT = 5;
    protected final static int POLLING_TIMEOUT = 30;
    public final Server server;
    protected final Handler handler;
    protected final OkHttpClient client;
    protected final SharedPreferences preferences;
    protected final ExecutorService executor = Executors.newFixedThreadPool(5);

    Pyx(Context context) {
        this.handler = new Handler(context.getMainLooper());
        this.server = Server.lastServer(context);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.client = new OkHttpClient.Builder().build();
    }

    protected Pyx(Server server, Handler handler, OkHttpClient client, SharedPreferences preferences) {
        this.server = server;
        this.handler = handler;
        this.client = client;
        this.preferences = preferences;
    }

    @NonNull
    public static Pyx get() throws LevelMismatchException {
        return InstanceHolder.holder().get(InstanceHolder.Level.STANDARD);
    }

    @NonNull
    public static Pyx get(Context context) {
        return InstanceHolder.holder().instantiateStandard(context);
    }

    public static void instantiate(Context context) {
        InstanceHolder.holder().instantiateStandard(context);
    }

    static void raiseException(JSONObject obj) throws PyxException {
        if (obj.optBoolean("e", false) || obj.has("ec")) throw new PyxException(obj);
    }

    public static void invalidate() {
        InstanceHolder.holder().invalidate();
    }

    protected void prepareRequest(@NonNull Op operation, @NonNull Request.Builder request) {
        if (operation == Op.FIRST_LOAD) {
            String lastSessionId = Prefs.getString(preferences, PKeys.LAST_JSESSIONID, null);
            if (lastSessionId != null) request.addHeader("Cookie", "JSESSIONID=" + lastSessionId);
        }
    }

    protected final PyxResponse request(@NonNull Op operation, NameValuePair... params) throws IOException, JSONException, PyxException {
        return request(operation, false, params);
    }

    @NonNull
    private PyxResponse request(@NonNull Op operation, boolean retried, NameValuePair... params) throws IOException, JSONException, PyxException {
        FormBody.Builder reqBody = new FormBody.Builder(Charset.forName("UTF-8")).add("o", operation.val);
        for (NameValuePair pair : params) reqBody.add(pair.key(), pair.value(""));

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
                    Logging.log(operation + "; " + Arrays.toString(params), false);
                } catch (PyxException ex) {
                    Logging.log("op = " + operation + ", params = " + Arrays.toString(params) + ", code = " + ex.errorCode + ", retried = " + retried, true);
                    if (!retried) return request(operation, true, params);
                    throw ex;
                }

                return new PyxResponse(resp, obj);
            } else {
                throw new StatusCodeException(resp);
            }
        }
    }

    public final void request(PyxRequest request, OnSuccess listener) {
        executor.execute(new RequestRunner(request, listener));
    }

    public final void requestSync(PyxRequest request) throws JSONException, PyxException, IOException {
        request(request.op, request.params);
    }

    public final <E> void request(PyxRequestWithResult<E> request, OnResult<E> listener) {
        executor.execute(new RequestWithResultRunner<>(request, listener));
    }

    @NonNull
    public final <E> E requestSync(PyxRequestWithResult<E> request) throws JSONException, PyxException, IOException {
        PyxResponse resp = request(request.op, request.params);
        return request.processor.process(preferences, resp.resp, resp.obj);
    }

    public final void firstLoad(final OnResult<FirstLoadedPyx> listener) {
        final InstanceHolder holder = InstanceHolder.holder();

        try {
            listener.onDone((FirstLoadedPyx) holder.get(InstanceHolder.Level.FIRST_LOADED));
        } catch (LevelMismatchException exx) {
            request(PyxRequests.firstLoad(), new OnResult<FirstLoad>() {
                @Override
                public void onDone(@NonNull FirstLoad result) {
                    FirstLoadedPyx pyx = new FirstLoadedPyx(server, handler, client, preferences, result);
                    holder.set(pyx);
                    listener.onDone(pyx);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    listener.onException(ex);
                }
            });
        }
    }

    @Override
    public void close() {
        client.dispatcher().executorService().shutdown();
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
        LIST_CARDCAST_CARD_SETS("clc"),
        ADD_CARDCAST_CARD_SET("cac"),
        REMOVE_CARDCAST_CARD_SET("crc");

        private final String val;

        Op(@NonNull String val) {
            this.val = val;
        }
    }

    public interface Processor<E> {
        @NonNull
        E process(@NonNull SharedPreferences prefs, @NonNull Response response, @NonNull JSONObject obj) throws JSONException;
    }

    public interface OnSuccess {
        void onDone();

        void onException(@NonNull Exception ex);
    }

    public interface OnResult<E> {
        void onDone(@NonNull E result);

        void onException(@NonNull Exception ex);
    }

    public interface OnEventListener {
        void onPollMessage(PollMessage message) throws JSONException;

        void onStoppedPolling();
    }

    protected static class PyxResponse {
        private final Response resp;
        private final JSONObject obj;

        PyxResponse(Response resp, JSONObject obj) {
            this.resp = resp;
            this.obj = obj;
        }
    }

    public static class Server {
        public final static Map<String, Server> pyxServers = new HashMap<>();
        private static final Pattern URL_PATTERN = Pattern.compile("pyx-(\\d)\\.pretendyoure\\.xyz");

        static {
            try {
                pyxServers.put("PYX1", new Server(parseUrlOrThrow("https://pyx-1.pretendyoure.xyz/zy/"), "The Biggest, Blackest Dick"));
                pyxServers.put("PYX2", new Server(parseUrlOrThrow("https://pyx-2.pretendyoure.xyz/zy/"), "A Falcon with a Box on its Head"));
                pyxServers.put("PYX3", new Server(parseUrlOrThrow("https://pyx-3.pretendyoure.xyz/zy/"), "Dickfingers"));
            } catch (JSONException ex) {
                Logging.log(ex);
            }
        }

        public final HttpUrl url;
        public final String name;
        public ServersChecker.CheckResult status = null;
        private HttpUrl ajaxUrl;
        private HttpUrl pollingUrl;

        public Server(@NonNull HttpUrl url, @NonNull String name) {
            this.url = url;
            this.name = name;
        }

        Server(JSONObject obj) throws JSONException {
            this(parseUrlOrThrow(obj.getString("uri")), obj.getString("name"));
        }

        @Nullable
        public static Server fromPyxUrl(String url) {
            Matcher matcher = URL_PATTERN.matcher(url);
            if (matcher.find()) {
                switch (matcher.group(1)) {
                    case "1":
                        return pyxServers.get("PYX1");
                    case "2":
                        return pyxServers.get("PYX2");
                    case "3":
                        return pyxServers.get("PYX3");
                }
            }

            return null;
        }

        public static int indexOf(List<Server> servers, String name) {
            for (int i = 0; i < servers.size(); i++)
                if (Objects.equals(servers.get(i).name, name))
                    return i;

            return -1;
        }

        @NonNull
        private static HttpUrl parseUrlOrThrow(String str) throws JSONException {
            if (str == null) throw new JSONException("str is null");

            try {
                HttpUrl url = HttpUrl.parse(str);
                if (url == null) throw new JSONException("Invalid url: " + str);
                return url;
            } catch (IllegalStateException ex) {
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
                Logging.log(ex);
                return null;
            }
        }

        @NonNull
        public static List<Server> loadUserServers(Context context) {
            List<Server> servers = new ArrayList<>();
            JSONArray array;
            try {
                array = Prefs.getJSONArray(context, PKeys.USER_SERVERS, new JSONArray());
            } catch (JSONException ex) {
                Logging.log(ex);
                return new ArrayList<>();
            }

            for (int i = 0; i < array.length(); i++) {
                try {
                    servers.add(new Server(array.getJSONObject(i)));
                } catch (JSONException ex) {
                    Logging.log(ex);
                }
            }

            return servers;
        }

        @Nullable
        private static Server getUserServer(Context context, String name) throws JSONException {
            JSONArray array = Prefs.getJSONArray(context, PKeys.USER_SERVERS, new JSONArray());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (Objects.equals(obj.optString("name"), name))
                    return new Server(obj);
            }

            return null;
        }

        @NonNull
        private static Server lastServer(Context context) {
            String name = Prefs.getString(context, PKeys.LAST_SERVER, "PYX1");

            Server server = null;
            for (Server pyxServer : pyxServers.values())
                if (Objects.equals(pyxServer.name, name))
                    server = pyxServer;

            if (server == null) {
                try {
                    server = getUserServer(context, name);
                } catch (JSONException ex) {
                    Logging.log(ex);
                }
            }

            if (server == null) server = pyxServers.get("PYX1");

            return server;
        }

        public static void addServer(Context context, Server server) throws JSONException {
            JSONArray array = Prefs.getJSONArray(context, PKeys.USER_SERVERS, new JSONArray());
            for (int i = array.length() - 1; i >= 0; i--) {
                if (Objects.equals(array.getJSONObject(i).getString("name"), server.name))
                    array.remove(i);
            }

            array.put(server.toJson());
            Prefs.putJSONArray(context, PKeys.USER_SERVERS, array);
        }

        public static void removeServer(Context context, Server server) {
            try {
                JSONArray array = Prefs.getJSONArray(context, PKeys.USER_SERVERS, new JSONArray());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    if (Objects.equals(obj.optString("name"), server.name)) {
                        array.remove(i);
                        break;
                    }
                }

                Prefs.putJSONArray(context, PKeys.USER_SERVERS, array);
            } catch (JSONException ex) {
                Logging.log(ex);
            }
        }

        public static boolean hasServer(Context context, String name) {
            try {
                return getUserServer(context, name) != null;
            } catch (JSONException ex) {
                Logging.log(ex);
                return true;
            }
        }

        @NonNull
        public HttpUrl ajax() {
            if (ajaxUrl == null)
                ajaxUrl = url.newBuilder().addPathSegment("AjaxServlet").build();

            return ajaxUrl;
        }

        @NonNull
        public HttpUrl polling() {
            if (pollingUrl == null)
                pollingUrl = url.newBuilder().addPathSegment("LongPollServlet").build();

            return pollingUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Server server = (Server) o;
            return url.equals(server.url) && name.equals(server.name);
        }

        @Override
        public String toString() {
            return name;
        }

        private JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("name", name)
                    .put("uri", url.toString());
        }
    }

    private class RequestRunner implements Runnable {
        private final PyxRequest request;
        private final OnSuccess listener;

        RequestRunner(PyxRequest request, OnSuccess listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                request(request.op, request.params);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDone();
                    }
                });
            } catch (IOException | JSONException | PyxException ex) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onException(ex);
                    }
                });
            }
        }
    }

    private class RequestWithResultRunner<E> implements Runnable {
        private final PyxRequestWithResult<E> request;
        private final OnResult<E> listener;

        RequestWithResultRunner(PyxRequestWithResult<E> request, OnResult<E> listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                PyxResponse resp = request(request.op, request.params);
                final E result = request.processor.process(preferences, resp.resp, resp.obj);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDone(result);
                    }
                });
            } catch (IOException | JSONException | PyxException ex) {
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
