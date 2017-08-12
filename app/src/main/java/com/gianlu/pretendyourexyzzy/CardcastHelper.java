package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.cardcastapi.Cardcast;
import com.gianlu.cardcastapi.Models.Decks;

import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CardcastHelper {
    private final Cardcast cardcast;
    private final Handler handler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CardcastHelper(Context context, Cardcast cardcast) {
        this.cardcast = cardcast;
        this.handler = new Handler(context.getMainLooper());
    }

    public void getDecks(final Search search, final int limit, final int offset, final IDecks listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Decks decks = cardcast.getDecks(search.query, search.categories, search.direction, search.sort, limit, offset, search.nsfw);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(search, decks);
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

    public interface IDecks {
        void onDone(Search search, Decks decks);

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
