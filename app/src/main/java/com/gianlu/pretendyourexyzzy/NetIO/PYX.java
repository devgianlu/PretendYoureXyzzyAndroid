package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamesList;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.Prefs;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler;
    private final Servers server;
    private final HttpClient client;
    private final CookieStore cookieStore;
    private final HttpContext httpContext;
    public PollingThread pollingThread;
    public FirstLoad firstLoad;

    private PYX(Context context) {
        handler = new Handler(context.getMainLooper());
        server = Servers.valueOf(Prefs.getString(context, Prefs.Keys.LAST_SERVER, Servers.PYX1.name()));
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
            throw new IllegalStateException("You must call #firstLoad first!!");
        HttpPost post = new HttpPost(server.addr + "AjaxServlet");
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

    public enum OP {
        REGISTER("r"),
        FIRST_LOAD("fl"),
        LOGOUT("lo"),
        GET_GAMES_LIST("ggl"),
        CHAT("c");

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
        PYX1("http://pyx-1.pretendyoure.xyz/zy/", "The Biggest, Blackest Dick"),
        PYX2("http://pyx-2.pretendyoure.xyz/zy/", "A Falcon with a Box on its Head"),
        PYX3("http://pyx-3.pretendyoure.xyz/zy/", "Dickfingers");

        public final String addr;
        public final String name;

        Servers(String addr, String name) {
            this.addr = addr;
            this.name = name;
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
        private boolean shouldStop = false;
        private List<IResult<List<PollMessage>>> listeners = new ArrayList<>();

        @Override
        public void run() {
            while (!shouldStop) {
                try {
                    HttpPost post = new HttpPost(server.addr + "LongPollServlet");
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
                    for (IResult<List<PollMessage>> listener : listeners)
                        listener.onDone(instance, obj);
                }
            });
        }

        private void dispatchEx(final Exception ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (IResult<List<PollMessage>> listener : listeners)
                        listener.onException(ex);
                }
            });
        }

        public void addListener(IResult<List<PollMessage>> listener) {
            this.listeners.add(listener);
        }

        public void removeListener(IResult<List<PollMessage>> listener) {
            this.listeners.remove(listener);
        }

        public void safeStop() {
            shouldStop = true;
        }
    }
}
