package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.InfiniteRecyclerView;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDecks;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.Date;
import java.util.Random;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class CardcastDecksAdapter extends InfiniteRecyclerView.InfiniteAdapter<CardcastDecksAdapter.ViewHolder, CardcastDeck> {
    private final LayoutInflater inflater;
    private final Cardcast cardcast;
    private final Cardcast.Search search;
    private final int limit;
    private final IAdapter listener;
    private final Random random = new Random();

    public CardcastDecksAdapter(Context context, Cardcast cardcast, Cardcast.Search search, CardcastDecks items, int limit, IAdapter listener) {
        super(context, items, -1, -1, false);
        this.inflater = LayoutInflater.from(context);
        this.cardcast = cardcast;
        this.search = search;
        this.limit = limit;
        this.listener = listener;
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getItem().name.hashCode();
    }

    @Nullable
    @Override
    protected Date getDateFromItem(CardcastDeck item) {
        return null;
    }

    @Override
    protected void userBindViewHolder(ViewHolder holder, int position) {
        final CardcastDeck deck = items.get(position).getItem();

        holder.name.setText(deck.name);
        holder.author.setText(context.getString(R.string.byLowercase, deck.author.username));
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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onDeckSelected(deck);
            }
        });

        CommonUtils.setRecyclerViewTopMargin(context, holder);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(parent);
    }

    @Override
    protected void moreContent(int page, final IContentProvider<CardcastDeck> provider) {
        cardcast.getDecks(search, limit, limit * page, new Cardcast.IDecks() {
            @Override
            public void onDone(Cardcast.Search search, CardcastDecks decks) {
                provider.onMoreContent(decks);
            }

            @Override
            public void onException(Exception ex) {
                provider.onFailed(ex);
            }
        });
    }

    public interface IAdapter {
        void onDeckSelected(CardcastDeck deck);
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
