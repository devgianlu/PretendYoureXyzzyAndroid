package com.gianlu.pretendyourexyzzy.NetIO;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.LruCache;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NameValuePair;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCost;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeckInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDecks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Cardcast {
    private static final String BASE_URL = "https://api.cardcastgame.com/v1/";
    private static Cardcast instance;
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler handler;
    private final LruCache<String, CardcastDeckInfo> cachedDecks;

    private Cardcast() {
        this.client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.cachedDecks = new LruCache<>(20);
    }

    @SuppressLint("SimpleDateFormat")
    public static SimpleDateFormat getDefaultDateParser() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    @NonNull
    public static Cardcast get() {
        if (instance == null) instance = new Cardcast();
        return instance;
    }

    @NonNull
    private Object basicRequest(@NonNull String endpoint, List<NameValuePair> params) throws IOException, JSONException {
        Uri.Builder builder = Uri.parse(BASE_URL + endpoint).buildUpon();
        for (NameValuePair pair : params) builder.appendQueryParameter(pair.key(), pair.value(""));

        Request request = new Request.Builder()
                .get()
                .url(builder.toString())
                .build();

        try (Response resp = client.newCall(request).execute()) {
            if (resp.code() < 200 || resp.code() >= 300)
                throw new StatusCodeException(resp);

            ResponseBody body = resp.body();
            if (body == null) throw new IOException("Body is empty!");

            String json = body.string();
            if (json.startsWith("[")) return new JSONArray(json);
            else return new JSONObject(json);
        }
    }

    private Object basicRequest(@NonNull String endpoint, NameValuePair... params) throws IOException, JSONException {
        return basicRequest(endpoint, Arrays.asList(params));
    }

    public void getDecks(final Search search, final int limit, final int offset, final OnDecks listener) {
        final List<NameValuePair> params = new ArrayList<>();
        if (search.query != null) params.add(new NameValuePair("search", search.query));
        params.add(new NameValuePair("category", search.categories == null ? "" : CommonUtils.join(search.categories, ",")));
        params.add(new NameValuePair("direction", search.direction.val));
        params.add(new NameValuePair("sort", search.sort.val));
        params.add(new NameValuePair("limit", String.valueOf(limit)));
        params.add(new NameValuePair("offset", String.valueOf(offset)));
        params.add(new NameValuePair("nsfw", String.valueOf(search.nsfw)));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final CardcastDecks decks = new CardcastDecks((JSONObject) basicRequest("decks", params));
                    if (decks.isEmpty() && offset == 0 && Pattern.matches("(?:[a-zA-Z]|\\d){5}", search.query)) {
                        try {
                            final CardcastDeckInfo info = new CardcastDeckInfo((JSONObject) basicRequest("decks/" + search.query));
                            cachedDecks.put(info.code, info);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onDone(search, CardcastDecks.singleton(info));
                                }
                            });

                            return;
                        } catch (IOException | JSONException ex) {
                            Logging.log(ex);
                        }
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(search, decks);
                        }
                    });
                } catch (JSONException | ParseException | IOException ex) {
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

    public void getResponses(final String code, final OnResult<List<CardcastCard>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<CardcastCard> cards = CardcastCard.toCardsList(code, (JSONArray) basicRequest("decks/" + code + "/responses"));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(cards);
                        }
                    });
                } catch (IOException | JSONException | ParseException ex) {
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

    public void getCalls(final String code, final OnResult<List<CardcastCard>> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<CardcastCard> cards = CardcastCard.toCardsList(code, (JSONArray) basicRequest("decks/" + code + "/calls"));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(cards);
                        }
                    });
                } catch (IOException | JSONException | ParseException ex) {
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

    @WorkerThread
    public CardcastDeckInfo getDeckInfoHitCache(String code) throws IOException, JSONException, ParseException {
        CardcastDeckInfo deck = cachedDecks.get(code);
        if (deck != null) return deck;
        else return new CardcastDeckInfo((JSONObject) basicRequest("decks/" + code));
    }

    public void getDeckInfo(final String code, final OnResult<CardcastDeckInfo> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final CardcastDeckInfo info = new CardcastDeckInfo((JSONObject) basicRequest("decks/" + code));
                    cachedDecks.put(info.code, info);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(info);
                        }
                    });
                } catch (IOException | ParseException | JSONException ex) {
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

    public void getCost(final String code, final OnResult<CardcastCost> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final CardcastCost cost = new CardcastCost((JSONObject) basicRequest("decks/" + code + "/cost"));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(cost);
                        }
                    });
                } catch (IOException | JSONException ex) {
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

    public enum Category {
        BOOKS("books"),
        COMMUNITY("community"),
        GAMING("gaming"),
        MOVIES("movies"),
        MUSIC("music"),
        SPORTS("sports"),
        TECHNOLOGY("technology"),
        TELEVISION("television"),
        TRANSLATION("translation"),
        OTHER("other"),
        RANDOM("random");

        public final String val;

        Category(String val) {
            this.val = val;
        }

        @Nullable
        public static Category parse(String val) {
            if (val == null || val.equals("null")) return null;

            for (Category category : values())
                if (Objects.equals(category.val, val))
                    return category;

            throw new IllegalArgumentException("Cannot find a category for " + val);
        }

        @Override
        public String toString() {
            return val;
        }
    }

    public enum Direction {
        DESCENDANT("desc"),
        ASCENDANT("asc");

        private final String val;

        Direction(String val) {
            this.val = val;
        }
    }

    public enum Sort {
        RATING("rating"),
        NEWEST("created_at"),
        NAME("name"),
        SIZE("card_count");

        public final String val;

        Sort(String val) {
            this.val = val;
        }
    }

    public interface OnDecks {
        @UiThread
        void onDone(@NonNull Search search, @NonNull CardcastDecks decks);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    public interface OnResult<E> {
        @UiThread
        void onDone(@NonNull E result);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    public static class Search {
        public final String query;
        public final List<Cardcast.Category> categories;
        public final Cardcast.Direction direction;
        public final Cardcast.Sort sort;
        public final boolean nsfw;

        public Search(@Nullable String query, @Nullable List<Cardcast.Category> categories, @NonNull Cardcast.Direction direction, @NonNull Cardcast.Sort sort, boolean nsfw) {
            this.query = query;
            this.categories = categories;
            this.direction = direction;
            this.sort = sort;
            this.nsfw = nsfw;
        }
    }
}
