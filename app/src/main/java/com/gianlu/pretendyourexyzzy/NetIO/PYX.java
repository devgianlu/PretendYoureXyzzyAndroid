package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.Adapters.GeneralItemsAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NameValuePair;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.PKeys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;


public class PYX {
    private final static int AJAX_TIMEOUT = 5;
    private final static int POLLING_TIMEOUT = 30;
    private static PYX instance;
    public final Server server;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Handler handler;
    private final OkHttpClient client;
    private final Object sessionIdLock = new Object();
    private final SharedPreferences preferences;
    public FirstLoad firstLoad;
    private PollingThread pollingThread;
    private boolean hasRetriedFirstLoad = false;
    private String sessionId = null;

    private PYX(Context context) {
        this.handler = new Handler(context.getMainLooper());
        this.server = Server.lastServer(context);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.client = new OkHttpClient.Builder().build();

        synchronized (sessionIdLock) {
            this.sessionId = Prefs.getString(preferences, PKeys.LAST_JSESSIONID, null);
        }
    }

    public static void invalidate() {
        instance = null;
    }

    public static PYX get(Context context) {
        if (instance == null) instance = new PYX(context);
        return instance;
    }

    private static void raiseException(JSONObject obj) throws PYXException {
        if (obj.optBoolean("e", false) || obj.has("ec")) throw new PYXException(obj);
    }

    @Nullable
    private static String findSessionId(Response response) {
        for (String cookie : response.headers("Set-Cookie")) {
            String[] segments = cookie.split(";");
            String[] keyValue = segments[0].split("=");
            if (Objects.equals(keyValue[0], "JSESSIONID"))
                return keyValue[1];
        }

        return null;
    }

    @NonNull
    public PollingThread getPollingThread() {
        if (pollingThread == null) startPolling();
        return pollingThread;
    }

    private JSONObject ajaxServletRequestSync(OP operation, NameValuePair... params) throws IOException, JSONException, PYXException {
        FormBody.Builder reqBody = new FormBody.Builder(Charset.forName("UTF-8")).add("o", operation.val);
        for (NameValuePair pair : params) reqBody.add(pair.key(), pair.value(""));

        Request.Builder builder = new Request.Builder()
                .url(server.url.newBuilder().addPathSegment("AjaxServlet").build())
                .post(reqBody.build());

        synchronized (sessionIdLock) {
            if (sessionId != null)
                builder.header("Cookie", "JSESSIONID=" + sessionId);
        }

        try (Response resp = client.newBuilder()
                .connectTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(AJAX_TIMEOUT, TimeUnit.SECONDS)
                .build().newCall(builder.build()).execute()) {

            if (operation == OP.REGISTER) {
                synchronized (sessionIdLock) {
                    sessionId = findSessionId(resp);
                }

                updateLastSessionId();
            }

            ResponseBody respBody = resp.body();
            if (respBody != null) {
                JSONObject obj = new JSONObject(respBody.string());

                try {
                    raiseException(obj);
                } catch (PYXException ex) {
                    Logging.log(operation + "; " + Arrays.toString(params) + "; " + ex.errorCode + "; " + hasRetriedFirstLoad, true);

                    if (operation == OP.FIRST_LOAD && !hasRetriedFirstLoad && Objects.equals(ex.errorCode, "se")) {
                        hasRetriedFirstLoad = true;
                        return ajaxServletRequestSync(operation, params);
                    }

                    throw ex;
                }

                Logging.log(operation + "; " + Arrays.toString(params), false);

                return obj;
            } else {
                throw new StatusCodeException(resp);
            }
        }
    }

    public void startPolling() {
        pollingThread = new PollingThread();
        pollingThread.start();
    }

    public void firstLoad(final IResult<FirstLoad> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.FIRST_LOAD);
                    final FirstLoad result = new FirstLoad(obj);
                    firstLoad = result;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, result);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    private User registerUserSync(@NonNull final String nickname) throws JSONException, PYXException, IOException {
        JSONObject obj = ajaxServletRequestSync(OP.REGISTER, new NameValuePair("n", nickname));
        final String confirmNick = obj.getString("n");
        if (!Objects.equals(confirmNick, nickname.trim())) throw new RuntimeException("WTF?!");
        return new User(confirmNick);
    }

    public void registerUser(final String nickname, final IResult<User> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final User user = registerUserSync(nickname);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, user);
                        }
                    });
                } catch (PYXException | IOException | JSONException ex) {
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

    private void updateLastSessionId() {
        Prefs.putString(preferences, PKeys.LAST_JSESSIONID, sessionId);
    }

    private void removeLastSessionId() {
        Prefs.remove(preferences, PKeys.LAST_JSESSIONID);
    }

    public void logout() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (pollingThread != null) {
                        pollingThread.safeStop();
                        pollingThread = null;
                    }

                    ajaxServletRequestSync(OP.LOGOUT);

                    removeLastSessionId();
                    firstLoad = null;
                } catch (IOException | JSONException | PYXException ignored) {
                }
            }
        });
    }

    public void getGamesList(final IResult<GamesList> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.GET_GAMES_LIST);

                    JSONArray gamesArray = obj.getJSONArray("gl");
                    final GamesList games = new GamesList(obj.getInt("mg"));
                    for (int i = 0; i < gamesArray.length(); i++)
                        games.add(new Game(gamesArray.getJSONObject(i)));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, games);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void sendGameMessage(final int gid, final String message, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.GAME_CHAT,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("m", message),
                            new NameValuePair("me", "false"));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void joinGame(final int gid, @Nullable final String password, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.JOIN_GAME,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("pw", password));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void spectateGame(final int gid, @Nullable final String password, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.SPECTATE_GAME,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("pw", password));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void leaveGame(final int gid, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.LEAVE_GAME, new NameValuePair("gid", String.valueOf(gid)));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (ex instanceof PYXException && Objects.equals(((PYXException) ex).errorCode, "nitg"))
                                listener.onDone(instance);
                            else listener.onException(ex);
                        }
                    });
                }
            }
        });
    }

    public void getGameInfoAndCards(final int gid, final IGameInfoAndCards listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject infoObj = ajaxServletRequestSync(OP.GET_GAME_INFO, new NameValuePair("gid", String.valueOf(gid)));
                    final GameInfo info = new GameInfo(infoObj);

                    JSONObject cardsObj = ajaxServletRequestSync(OP.GET_GAME_CARDS, new NameValuePair("gid", String.valueOf(gid)));
                    final GameCards cards = new GameCards(cardsObj);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onGameInfoAndCards(info, cards);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void getGameInfo(final int gid, final IResult<GameInfo> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.GET_GAME_INFO, new NameValuePair("gid", String.valueOf(gid)));
                    final GameInfo info = new GameInfo(obj);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, info);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void changeGameOptions(final int gid, final Game.Options options, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.CHANGE_GAME_OPTIONS,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("go", options.toJson().toString()));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void getNamesList(final IResult<List<String>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.GET_NAMES_LIST);
                    final List<String> names = CommonUtils.toStringsList(obj.getJSONArray("nl"), true);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, names);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void playCard(final int gid, final int cid, @Nullable final String customText, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.PLAY_CARD,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("cid", String.valueOf(cid)),
                            new NameValuePair("m", customText));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void judgeCard(final int gid, final int cid, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.JUDGE_SELECT,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("cid", String.valueOf(cid)));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void createGame(final IResult<Integer> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.CREATE_GAME);
                    final int gid = obj.getInt("gid");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, gid);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void startGame(final int gid, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.START_GAME, new NameValuePair("gid", String.valueOf(gid)));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void listCardcastCardSets(final int gid, @Nullable final Cardcast cardcast, final IResult<List<CardSet>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.LIST_CARDCAST_CARD_SETS, new NameValuePair("gid", String.valueOf(gid)));
                    final List<CardSet> sets = CommonUtils.toTList(obj.getJSONArray("css"), CardSet.class);

                    if (cardcast != null)
                        for (CardSet set : sets)
                            set.cardcastDeck = cardcast.guessDeckSync(set);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(PYX.this, sets);
                        }
                    });
                } catch (IOException | JSONException | PYXException | URISyntaxException | ParseException ex) {
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

    public void addCardcastCardSetSync(int gid, String code) throws JSONException, PYXException, IOException {
        ajaxServletRequestSync(OP.ADD_CARDCAST_CARD_SET,
                new NameValuePair("gid", String.valueOf(gid)),
                new NameValuePair("cci", code));
    }

    public void addCardcastCardSet(final int gid, final String code, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    addCardcastCardSetSync(gid, code);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public void removeCardcastCardSet(final int gid, final String code, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.REMOVE_CARDCAST_CARD_SET,
                            new NameValuePair("gid", String.valueOf(gid)),
                            new NameValuePair("cci", code));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance);
                        }
                    });
                } catch (IOException | JSONException | PYXException ex) {
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

    public enum OP {
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

        OP(String val) {
            this.val = val;
        }
    }

    public interface IGameInfoAndCards {
        void onGameInfoAndCards(GameInfo info, GameCards cards);

        void onException(Exception ex);
    }

    public interface ISuccess {
        void onDone(PYX pyx);

        void onException(Exception ex);
    }

    public interface IResult<E> {
        void onDone(PYX pyx, E result);

        void onException(Exception ex);
    }

    public interface IEventListener {
        void onPollMessage(PollMessage message) throws JSONException;

        void onStoppedPolling();
    }

    public static class Server implements GeneralItemsAdapter.Item {
        public final static Map<String, Server> pyxServers = new HashMap<>();
        private static final Pattern URL_PATTERN = Pattern.compile("pyx-(\\d)\\.pretendyoure\\.xyz");

        static {
            try {
                pyxServers.put("PYX1", new Server(HttpUrl.parse("https://pyx-1.pretendyoure.xyz/zy/"), "The Biggest, Blackest Dick"));
                pyxServers.put("PYX2", new Server(HttpUrl.parse("https://pyx-2.pretendyoure.xyz/zy/"), "A Falcon with a Box on its Head"));
                pyxServers.put("PYX3", new Server(HttpUrl.parse("https://pyx-3.pretendyoure.xyz/zy/"), "Dickfingers"));
            } catch (MalformedURLException ignored) {
            }
        }

        public final HttpUrl url;
        public final String name;

        public Server(HttpUrl url, String name) throws MalformedURLException {
            if (url == null) throw new MalformedURLException("Invalid url!");
            this.url = url;
            this.name = name;
        }

        Server(JSONObject obj) throws JSONException, MalformedURLException {
            this(HttpUrl.parse(obj.getString("uri")), obj.getString("name"));
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
                } catch (JSONException | MalformedURLException ex) {
                    Logging.log(ex);
                }
            }

            return servers;
        }

        public static boolean isNameOk(Context context, String name) {
            try {
                return !(name == null || name.isEmpty() || getUserServer(context, name) != null);
            } catch (Exception ex) {
                Logging.log(ex);
                return false;
            }
        }

        @Nullable
        static Server getUserServer(Context context, String name) throws JSONException, MalformedURLException {
            JSONArray array = Prefs.getJSONArray(context, PKeys.USER_SERVERS, new JSONArray());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (Objects.equals(obj.optString("name"), name))
                    return new Server(obj);
            }

            return null;
        }

        @NonNull
        static Server lastServer(Context context) {
            String name = Prefs.getString(context, PKeys.LAST_SERVER, "PYX1");

            Server server = null;
            for (Server pyxServer : pyxServers.values())
                if (Objects.equals(pyxServer.name, name))
                    server = pyxServer;

            if (server == null) {
                try {
                    server = getUserServer(context, name);
                } catch (JSONException | MalformedURLException ex) {
                    Logging.log(ex);
                }
            }

            if (server == null) server = pyxServers.get("PYX1");

            return server;
        }

        public static void addServer(Context context, Server server) throws JSONException {
            JSONArray array = Prefs.getJSONArray(context, PKeys.USER_SERVERS, new JSONArray());
            array.put(server.toJson());
            Prefs.putJSONArray(context, PKeys.USER_SERVERS, array);
        }

        public static void clearUserServers(Context context) {
            Prefs.remove(context, PKeys.USER_SERVERS);
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

        @Override
        public String toString() {
            return name;
        }

        private JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("name", name)
                    .put("uri", url.toString());
        }

        @Override
        public String getPrimaryText() {
            return name;
        }

        @Nullable
        @Override
        public String getSecondaryText() {
            return url.toString();
        }
    }

    public class PollingThread extends Thread {
        private final WeakHashMap<String, IEventListener> listeners = new WeakHashMap<>();
        private final AtomicInteger exCount = new AtomicInteger(0);
        private boolean shouldStop = false;

        @Override
        public void run() {
            while (!shouldStop) {
                try {
                    Request.Builder builder = new Request.Builder()
                            .post(Util.EMPTY_REQUEST)
                            .url(server.url.newBuilder().addPathSegment("LongPollServlet").build());

                    synchronized (sessionIdLock) {
                        builder.header("Cookie", "JSESSIONID=" + sessionId);
                    }

                    try (Response resp = client.newBuilder()
                            .connectTimeout(POLLING_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(POLLING_TIMEOUT, TimeUnit.SECONDS)
                            .build().newCall(builder.build()).execute()) {
                        if (resp.code() != 200) throw new StatusCodeException(resp);

                        String json;
                        ResponseBody body = resp.body();
                        if (body != null) json = body.string();
                        else throw new IOException("Body is empty!");

                        if (json.startsWith("{")) {
                            JSONObject obj = new JSONObject(json);
                            raiseException(obj);
                        } else if (json.startsWith("[")) {
                            JSONArray array = new JSONArray(json);
                            dispatchDone(CommonUtils.toTList(array, PollMessage.class));
                        }
                    }
                } catch (IOException | JSONException | PYXException ex) {
                    dispatchEx(ex);
                }
            }
        }

        private void dispatchDone(final List<PollMessage> obj) {
            exCount.set(0);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (IEventListener listener : listeners.values()) {
                        for (PollMessage message : obj) {
                            try {
                                listener.onPollMessage(message);
                            } catch (JSONException ex) {
                                dispatchEx(ex);
                            }
                        }
                    }
                }
            });
        }

        private void dispatchEx(final Exception ex) {
            exCount.getAndIncrement();
            if (exCount.get() > 5) {
                safeStop();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (IEventListener listener : listeners.values())
                            listener.onStoppedPolling();
                    }
                });
            }

            Logging.log(ex);
        }

        public void addListener(String tag, IEventListener listener) {
            this.listeners.put(tag, listener);
        }

        void safeStop() {
            shouldStop = true;
        }
    }
}
