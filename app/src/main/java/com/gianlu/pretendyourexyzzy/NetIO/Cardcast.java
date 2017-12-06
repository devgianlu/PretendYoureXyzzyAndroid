package com.gianlu.pretendyourexyzzy.NetIO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Prefs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCost;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeckInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDecks;
import com.gianlu.pretendyourexyzzy.PKeys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

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
    private final LruCache<String, String> cachedDeckNames;
    private final SharedPreferences preferences;

    private Cardcast(Context context) {
        this.client = HttpClients.createDefault();
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.cachedDeckNames = new LruCache<>(100);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadCache();
    }

    @SuppressLint("SimpleDateFormat")
    public static SimpleDateFormat getDefaultDateParser() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    public static Cardcast get(Context context) {
        if (instance == null) instance = new Cardcast(context);
        return instance;
    }

    @Nullable
    private CardcastDeck findDeck(CardcastDecks decks, CardSet set) {
        if (decks.size() == 1) {
            return decks.get(0);
        } else {
            for (CardcastDeck deck : decks) {
                if (deck.name.equals(set.name) && deck.calls == set.blackCards && deck.responses == set.whiteCards)
                    return deck;
            }
        }

        return null;
    }

    private void loadCache() {
        Set<String> names = Prefs.getSet(preferences, PKeys.CACHED_DECK_NAMES, new HashSet<String>());
        Iterator<String> namesIter = names.iterator();
        Set<String> codes = Prefs.getSet(preferences, PKeys.CACHED_DECK_CODES, new HashSet<String>());
        Iterator<String> codesIter = codes.iterator();

        while (namesIter.hasNext() && codesIter.hasNext())
            cachedDeckNames.put(namesIter.next(), codesIter.next());
    }

    private void addCachedDeckName(String name, String code) {
        cachedDeckNames.put(name, code);

        Map<String, String> snapshot = cachedDeckNames.snapshot();
        Prefs.putSet(preferences, PKeys.CACHED_DECK_NAMES, snapshot.keySet());
        Prefs.putSet(preferences, PKeys.CACHED_DECK_CODES, new HashSet<>(snapshot.values()));
    }

    public CardcastDeck guessDeckSync(final CardSet set) throws JSONException, IOException, URISyntaxException, ParseException {
        String cachedCode = cachedDeckNames.get(set.name);
        if (cachedCode != null) {
            return new CardcastDeckInfo((JSONObject) basicRequest("decks/" + cachedCode));
        } else {
            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("search", set.name));

            final CardcastDecks decks = new CardcastDecks((JSONObject) basicRequest("decks", params));
            final CardcastDeck match = findDeck(decks, set);
            if (match != null) addCachedDeckName(set.name, match.code);
            return match;
        }
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
                    if (decks.isEmpty() && Pattern.matches("(?:[a-zA-Z]|\\d){5}", search.query)) {
                        final CardcastDeckInfo info = new CardcastDeckInfo((JSONObject) basicRequest("decks/" + search.query));
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onDone(search, CardcastDecks.singleton(info));
                            }
                        });

                        return;
                    }

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
