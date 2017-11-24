package com.gianlu.pretendyourexyzzy.NetIO;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCost;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeckInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDecks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class Cardcast {
    public static final String BASE_URL = "https://api.cardcastgame.com/v1/";
    private static Cardcast instance;
    private final HttpClient client;
    private final ExecutorService executor;
    private final Handler handler;

    private Cardcast() {
        this.client = HttpClients.createDefault();
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("SimpleDateFormat")
    public static SimpleDateFormat getDefaultDateParser() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    public static Cardcast get() {
        if (instance == null) instance = new Cardcast();
        return instance;
    }

    @NonNull
    private Object basicRequest(@NonNull String endpoint, List<NameValuePair> params) throws URISyntaxException, IOException, JSONException {
        URIBuilder builder = new URIBuilder(BASE_URL + endpoint);
        builder.setParameters(params);
        HttpGet get = new HttpGet(builder.build());

        HttpResponse resp = client.execute(get);

        StatusLine sl = resp.getStatusLine();
        if (sl.getStatusCode() < HttpStatus.SC_OK || sl.getStatusCode() >= HttpStatus.SC_MULTIPLE_CHOICES)
            throw new StatusCodeException(sl);

        String json = EntityUtils.toString(resp.getEntity());
        if (json.startsWith("[")) return new JSONArray(json);
        else return new JSONObject(json);
    }

    private Object basicRequest(@NonNull String endpoint, NameValuePair... params) throws URISyntaxException, IOException, JSONException {
        return basicRequest(endpoint, Arrays.asList(params));
    }

    public void getDecks(final Search search, final int limit, final int offset, final IDecks listener) {
        final List<NameValuePair> params = new ArrayList<>();
        if (search.query != null) params.add(new BasicNameValuePair("search", search.query));
        params.add(new BasicNameValuePair("category", search.categories == null ? "" : CommonUtils.join(search.categories, ",")));
        params.add(new BasicNameValuePair("direction", search.direction.val));
        params.add(new BasicNameValuePair("sort", search.sort.val));
        params.add(new BasicNameValuePair("limit", String.valueOf(limit)));
        params.add(new BasicNameValuePair("offset", String.valueOf(offset)));
        params.add(new BasicNameValuePair("nsfw", String.valueOf(search.nsfw)));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final CardcastDecks decks = new CardcastDecks((JSONObject) basicRequest("decks", params));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(search, decks);
                        }
                    });
                } catch (JSONException | ParseException | URISyntaxException | IOException ex) {
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

    public void getResponses(final String code, final IResult<List<CardcastCard>> listener) {
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
                } catch (IOException | URISyntaxException | JSONException | ParseException ex) {
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

    public void getCalls(final String code, final IResult<List<CardcastCard>> listener) {
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
                } catch (IOException | URISyntaxException | JSONException | ParseException ex) {
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

    public void getDeckInfo(final String code, final IResult<CardcastDeckInfo> listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final CardcastDeckInfo info = new CardcastDeckInfo((JSONObject) basicRequest("decks/" + code));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(info);
                        }
                    });
                } catch (IOException | URISyntaxException | ParseException | JSONException ex) {
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

    public void getCost(final String code, final IResult<CardcastCost> listener) {
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
                } catch (IOException | URISyntaxException | JSONException ex) {
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

        public static Category parse(String val) {
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

    public interface IDecks {
        void onDone(Search search, CardcastDecks decks);

        void onException(Exception ex);
    }

    public interface IResult<E> {
        void onDone(E result);

        void onException(Exception ex);
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
