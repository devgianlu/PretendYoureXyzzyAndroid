package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CasualViews.InfiniteRecyclerView;
import com.gianlu.commonutils.CasualViews.SuperTextView;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDecks;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class CardcastDecksAdapter extends InfiniteRecyclerView.InfiniteAdapter<CardcastDecksAdapter.ViewHolder, CardcastDeck> {
    private final LayoutInflater inflater;
    private final Cardcast cardcast;
    private final Cardcast.Search search;
    private final int limit;
    private final Listener listener;
    private final Random random = ThreadLocalRandom.current();

    public CardcastDecksAdapter(Context context, Cardcast cardcast, Cardcast.Search search, CardcastDecks items, int limit, Listener listener) {
        super(context, new Config<CardcastDeck>().items(items).undeterminedPages().noSeparators());
        this.inflater = LayoutInflater.from(context);
        this.cardcast = cardcast;
        this.search = search;
        this.limit = limit;
        this.listener = listener;
    }

    @Nullable
    @Override
    protected Date getDateFromItem(CardcastDeck item) {
        return null;
    }

    @Override
    protected void userBindViewHolder(@NonNull ViewHolder holder, @NonNull ItemEnclosure<CardcastDeck> item, int position) {
        final CardcastDeck deck = item.getItem();

        holder.name.setText(deck.name);
        CommonUtils.setText(holder.author, R.string.byLowercase, deck.author.username);
        holder.nsfw.setVisibility(deck.hasNsfwCards ? View.VISIBLE : View.GONE);

        if (deck.sampleCalls != null && !deck.sampleCalls.isEmpty()
                && deck.sampleResponses != null && !deck.sampleResponses.isEmpty()) {
            CardcastCard exampleBlackCard = deck.sampleCalls.get(random.nextInt(deck.sampleCalls.size()));
            CardcastCard exampleWhiteCard = deck.sampleResponses.get(random.nextInt(deck.sampleResponses.size()));
            holder.example.setHtml(Utils.composeCardcastDeckSentence(exampleBlackCard, exampleWhiteCard));
            holder.example.setVisibility(View.VISIBLE);
        } else {
            holder.example.setVisibility(View.GONE);
        }

        holder.rating.setRating(deck.rating);
        holder.rating.setEnabled(false);
        holder.blackCards.setText(String.valueOf(deck.calls));
        holder.whiteCards.setText(String.valueOf(deck.responses));

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onDeckSelected(deck);
        });

        CommonUtils.setRecyclerViewTopMargin(holder);
    }

    @NonNull
    @Override
    protected RecyclerView.ViewHolder createViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(parent);
    }

    @Override
    protected void moreContent(int page, @NonNull ContentProvider<CardcastDeck> provider) {
        cardcast.getDecks(search, limit, limit * page, null, new Cardcast.OnDecks() {
            @Override
            public void onDone(@NonNull Cardcast.Search search, @NonNull CardcastDecks decks) {
                provider.onMoreContent(decks);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                provider.onFailed(ex);
            }
        });
    }

    public interface Listener {
        void onDeckSelected(@NonNull CardcastDeck deck);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView example;
        final TextView name;
        final TextView author;
        final View nsfw;
        final MaterialRatingBar rating;
        final TextView blackCards;
        final TextView whiteCards;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_cardcast_deck, parent, false));

            example = itemView.findViewById(R.id.cardcastDeckItem_example);
            name = itemView.findViewById(R.id.cardcastDeckItem_name);
            author = itemView.findViewById(R.id.cardcastDeckItem_author);
            nsfw = itemView.findViewById(R.id.cardcastDeckItem_nsfw);
            rating = itemView.findViewById(R.id.cardcastDeckItem_rating);
            blackCards = itemView.findViewById(R.id.cardcastDeckItem_blackCards);
            whiteCards = itemView.findViewById(R.id.cardcastDeckItem_whiteCards);
        }
    }
}
