package com.gianlu.pretendyourexyzzy.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.CardcastCard;
import com.gianlu.pretendyourexyzzy.api.models.CardcastCost;
import com.gianlu.pretendyourexyzzy.api.models.CardcastDeckInfo;
import com.gianlu.pretendyourexyzzy.api.models.CardcastDecks;

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
    private final LifecycleAwareHandler handler;
    private final LruCache<String, CardcastDeckInfo> cachedDecks;

    private Cardcast() {
        this.client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor()).build();
        this.handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
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
    private Object basicRequest(@NonNull String endpoint, List<Param> params) throws IOException, JSONException {
        Uri.Builder builder = Uri.parse(BASE_URL + endpoint).buildUpon();
        for (Param pair : params) builder.appendQueryParameter(pair.key(), pair.value(""));

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

    private Object basicRequest(@NonNull String endpoint, Param... params) throws IOException, JSONException {
        return basicRequest(endpoint, Arrays.asList(params));
    }

    public void getDecks(@NonNull Search search, int limit, int offset, @Nullable Activity activity, @NonNull OnDecks listener) {
        final List<Param> params = new ArrayList<>();
        if (search.query != null) params.add(new Param("search", search.query));
        params.add(new Param("category", search.categories == null ? "" : CommonUtils.join(search.categories, ",")));
        params.add(new Param("direction", search.direction.val));
        params.add(new Param("sort", search.sort.val));
        params.add(new Param("limit", String.valueOf(limit)));
        params.add(new Param("offset", String.valueOf(offset)));
        params.add(new Param("nsfw", String.valueOf(search.nsfw)));

        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    final CardcastDecks decks = new CardcastDecks((JSONObject) basicRequest("decks", params));
                    if (decks.isEmpty() && offset == 0 && search.query != null && Pattern.matches("(?:[a-zA-Z]|\\d){5}", search.query)) {
                        try {
                            CardcastDeckInfo info = new CardcastDeckInfo((JSONObject) basicRequest("decks/" + search.query));
                            cachedDecks.put(info.code, info);

                            post(() -> listener.onDone(search, CardcastDecks.singleton(info)));
                            return;
                        } catch (IOException | JSONException ex) {
                            Logging.log(ex);
                        }
                    }

                    post(() -> listener.onDone(search, decks));
                } catch (JSONException | ParseException | IOException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public void getResponses(@NonNull String code, @Nullable Activity activity, @NonNull OnResult<List<CardcastCard>> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    List<CardcastCard> cards = CardcastCard.toCardsList(code, (JSONArray) basicRequest("decks/" + code + "/responses"));
                    post(() -> listener.onDone(cards));
                } catch (IOException | JSONException | ParseException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public void getCalls(@NonNull String code, @Nullable Activity activity, @NonNull OnResult<List<CardcastCard>> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    List<CardcastCard> cards = CardcastCard.toCardsList(code, (JSONArray) basicRequest("decks/" + code + "/calls"));
                    post(() -> listener.onDone(cards));
                } catch (IOException | JSONException | ParseException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    @WorkerThread
    public CardcastDeckInfo getDeckInfoHitCache(@NonNull String code) throws IOException, JSONException, ParseException {
        CardcastDeckInfo deck = cachedDecks.get(code);
        if (deck != null) return deck;
        else return new CardcastDeckInfo((JSONObject) basicRequest("decks/" + code));
    }

    public void getDeckInfo(@NonNull String code, @Nullable Activity activity, @NonNull OnResult<CardcastDeckInfo> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    CardcastDeckInfo info = new CardcastDeckInfo((JSONObject) basicRequest("decks/" + code));
                    cachedDecks.put(info.code, info);
                    post(() -> listener.onDone(info));
                } catch (IOException | ParseException | JSONException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public void getCost(@NonNull String code, @Nullable Activity activity, @NonNull OnResult<CardcastCost> listener) {
        executor.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    CardcastCost cost = new CardcastCost((JSONObject) basicRequest("decks/" + code + "/cost"));
                    post(() -> listener.onDone(cost));
                } catch (IOException | JSONException ex) {
                    post(() -> listener.onException(ex));
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

        @NonNull
        public String getFormal(@NonNull Context context) {
            switch (this) {
                case BOOKS:
                    return context.getString(R.string.books);
                case COMMUNITY:
                    return context.getString(R.string.community);
                case GAMING:
                    return context.getString(R.string.gaming);
                case MOVIES:
                    return context.getString(R.string.movies);
                case MUSIC:
                    return context.getString(R.string.music);
                case SPORTS:
                    return context.getString(R.string.sports);
                case TECHNOLOGY:
                    return context.getString(R.string.technology);
                case TELEVISION:
                    return context.getString(R.string.television);
                case TRANSLATION:
                    return context.getString(R.string.translation);
                default:
                case OTHER:
                    return context.getString(R.string.other);
                case RANDOM:
                    return context.getString(R.string.random);
            }
        }

        @NonNull
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

    private static class Param {
        private final String key;
        private final String value;

        Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key() {
            return key;
        }

        String value() {
            return value;
        }

        String value(String fallback) {
            return value == null ? fallback : value;
        }
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
