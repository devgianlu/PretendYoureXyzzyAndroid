package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.PKeys;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.client.BasicCookieStore;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.BasicHttpContext;
import cz.msebera.android.httpclient.protocol.HttpContext;
import cz.msebera.android.httpclient.util.EntityUtils;

public class PYX {
    private static PYX instance;
    public final Servers server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler;
    private final HttpClient client;
    private final CookieStore cookieStore;
    private final HttpContext httpContext;
    public PollingThread pollingThread;
    public FirstLoad firstLoad;

    private PYX(Context context) {
        handler = new Handler(context.getMainLooper());
        server = Servers.valueOf(Prefs.getString(context, PKeys.LAST_SERVER, Servers.PYX1.name()));
        cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        client = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
    }

    public static void invalidate() {
        instance = null;
    }

    public static PYX get(Context context) {
        if (instance == null) instance = new PYX(context);
        return instance;
    }

    private static void raiseException(JSONObject obj) throws PYXException {
        if (obj.optBoolean("e", false)) throw new PYXException(obj);
    }

    private void addJESSIONIDCookie(HttpRequestBase request) {
        Cookie jSessionId = Utils.findCookie(cookieStore, "JSESSIONID");
        if (jSessionId == null) throw new NullPointerException("JSESSIONID can't be null!!");
        request.setHeader("Set-Cookie", "JSESSIONID=" + jSessionId.getValue());
    }

    private JSONObject ajaxServletRequestSync(OP operation, NameValuePair... params) throws IOException, JSONException, PYXException {
        if (operation != OP.FIRST_LOAD && firstLoad == null)
            throw new IOException(new IllegalStateException("You must call #firstLoad first!!"));
        HttpPost post = new HttpPost(server.uri.toString() + "AjaxServlet");
        List<NameValuePair> paramsList = new ArrayList<>(Arrays.asList(params));
        paramsList.add(new BasicNameValuePair("o", operation.val));
        post.setEntity(new UrlEncodedFormEntity(paramsList));

        if (operation != OP.FIRST_LOAD)
            addJESSIONIDCookie(post);

        HttpResponse resp = client.execute(post, httpContext);

        StatusLine sl = resp.getStatusLine();
        if (sl.getStatusCode() != HttpStatus.SC_OK)
            throw new StatusCodeException(sl);

        JSONObject obj = new JSONObject(EntityUtils.toString(resp.getEntity()));
        post.releaseConnection();
        raiseException(obj);
        return obj;
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
                    PYX.this.firstLoad = result;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance, result);
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

    public void registerUser(final String nickname, final IResult<User> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.REGISTER, new BasicNameValuePair("n", nickname));
                    final String confirmNick = obj.getString("n");
                    if (!Objects.equals(confirmNick, nickname)) throw new RuntimeException("WTF?!");
                    final User user = new User(confirmNick);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance, user);
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

    public void logout() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    pollingThread.safeStop();
                    pollingThread = null;
                    ajaxServletRequestSync(OP.LOGOUT);
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
                            listener.onDone(instance, games);
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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("m", message),
                            new BasicNameValuePair("me", "false"));

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

    public void sendMessage(final String message, final ISuccess listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ajaxServletRequestSync(OP.CHAT,
                            new BasicNameValuePair("m", message),
                            new BasicNameValuePair("me", "false"));

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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("pw", password));

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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("pw", password));

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
                    ajaxServletRequestSync(OP.LEAVE_GAME, new BasicNameValuePair("gid", String.valueOf(gid)));

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

    public void getGameInfo(final int gid, final IResult<GameInfo> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.GET_GAME_INFO, new BasicNameValuePair("gid", String.valueOf(gid)));
                    final GameInfo info = new GameInfo(obj);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance, info);
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

    public void getGameCards(final int gid, final IResult<GameCards> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.GET_GAME_CARDS, new BasicNameValuePair("gid", String.valueOf(gid)));
                    final GameCards cards = new GameCards(obj);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance, cards);
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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("go", options.toJSON().toString()));

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

    public void getNamesList(final IResult<List<String>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.GET_NAMES_LIST);

                    JSONArray namesArray = obj.getJSONArray("nl");
                    final List<String> names = new ArrayList<>();
                    for (int i = 0; i < namesArray.length(); i++) {
                        String name = namesArray.getString(i);
                        if (!names.contains(name)) names.add(name);
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance, names);
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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("cid", String.valueOf(cid)),
                            new BasicNameValuePair("m", customText));

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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("cid", String.valueOf(cid)));

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
                            listener.onDone(instance, gid);
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
                    ajaxServletRequestSync(OP.START_GAME, new BasicNameValuePair("gid", String.valueOf(gid)));

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

    public void listCardcastCardSets(final int gid, final IResult<List<CardSet>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = ajaxServletRequestSync(OP.LIST_CARDCAST_CARD_SETS, new BasicNameValuePair("gid", String.valueOf(gid)));
                    final List<CardSet> sets = CardSet.toCardSetsList(obj.getJSONArray("css"));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(instance, sets);
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

    public void addCardcastCardSetSync(int gid, String code) throws JSONException, PYXException, IOException {
        ajaxServletRequestSync(OP.ADD_CARDCAST_CARD_SET,
                new BasicNameValuePair("gid", String.valueOf(gid)),
                new BasicNameValuePair("cci", code));
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
                            new BasicNameValuePair("gid", String.valueOf(gid)),
                            new BasicNameValuePair("cci", code));

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

        @NonNull
        public static OP parse(String val) {
            for (OP op : values())
                if (Objects.equals(op.val, val))
                    return op;

            throw new IllegalArgumentException("Cannot find operation with value: " + val);
        }
    }

    public enum Servers {
        PYX1(URI.create("https://pyx-1.pretendyoure.xyz/zy/"), "The Biggest, Blackest Dick"),
        PYX2(URI.create("https://pyx-2.pretendyoure.xyz/zy/"), "A Falcon with a Box on its Head"),
        PYX3(URI.create("https://pyx-3.pretendyoure.xyz/zy/"), "Dickfingers");

        private static final Pattern URL_PATTERN = Pattern.compile("pyx-(\\d)\\.pretendyoure\\.xyz");
        public final URI uri;
        public final String name;


        Servers(URI uri, String name) {
            this.uri = uri;
            this.name = name;
        }

        @Nullable
        public static Servers fromUrl(String url) {
            Matcher matcher = URL_PATTERN.matcher(url);
            if (matcher.find()) {
                switch (matcher.group(1)) {
                    case "1":
                        return PYX1;
                    case "2":
                        return PYX2;
                    case "3":
                        return PYX3;
                }
            }

            return null;
        }

        public static String[] formalValues() {
            Servers[] values = values();
            String[] formalValues = new String[values.length];
            for (int i = 0; i < values.length; i++) formalValues[i] = values[i].name;
            return formalValues;
        }
    }

    public interface ISuccess {
        void onDone(PYX pyx);

        void onException(Exception ex);
    }

    public interface IResult<E> {
        void onDone(PYX pyx, E result);

        void onException(Exception ex);
    }

    public class PollingThread extends Thread {
        private final WeakHashMap<String, IResult<List<PollMessage>>> listeners = new WeakHashMap<>();
        private boolean shouldStop = false;

        @Override
        public void run() {
            while (!shouldStop) {
                try {
                    HttpPost post = new HttpPost(server.uri.toString() + "LongPollServlet");
                    addJESSIONIDCookie(post);

                    HttpResponse resp = client.execute(post, httpContext);

                    StatusLine sl = resp.getStatusLine();
                    if (sl.getStatusCode() != HttpStatus.SC_OK)
                        throw new StatusCodeException(sl);

                    String json = EntityUtils.toString(resp.getEntity());
                    post.releaseConnection();

                    if (json.startsWith("{")) {
                        JSONObject obj = new JSONObject(json);
                        raiseException(obj);
                    } else if (json.startsWith("[")) {
                        JSONArray array = new JSONArray(json);
                        dispatchDone(PollMessage.toPollMessagesList(array));
                    }
                } catch (final IOException | JSONException | PYXException ex) {
                    dispatchEx(ex);
                }
            }
        }

        private void dispatchDone(final List<PollMessage> obj) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (IResult<List<PollMessage>> listener : listeners.values())
                        listener.onDone(instance, obj);
                }
            });
        }

        private void dispatchEx(final Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (IResult<List<PollMessage>> listener : listeners.values())
                        listener.onException(ex);
                }
            });
        }

        public void addListener(String tag, IResult<List<PollMessage>> listener) {
            this.listeners.put(tag, listener);
        }

        public void removeListener(String tag) {
            this.listeners.remove(tag);
        }

        public void safeStop() {
            shouldStop = true;
        }
    }
}
