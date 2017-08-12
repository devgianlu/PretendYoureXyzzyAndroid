package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.cardcastapi.Models.Card;
import com.gianlu.cardcastapi.Models.Deck;
import com.gianlu.cardcastapi.Models.Decks;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.InfiniteRecyclerView;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.CardcastHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.Date;
import java.util.Random;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class CardcastDecksAdapter extends InfiniteRecyclerView.InfiniteAdapter<CardcastDecksAdapter.ViewHolder, Deck> {
    private final LayoutInflater inflater;
    private final CardcastHelper cardcast;
    private final CardcastHelper.Search search;
    private final int limit;
    private final IAdapter listener;
    private final Random random = new Random();

    public CardcastDecksAdapter(Context context, CardcastHelper cardcast, CardcastHelper.Search search, Decks items, int limit, IAdapter listener) {
        super(context, items, -1, -1, false);
        this.inflater = LayoutInflater.from(context);
        this.cardcast = cardcast;
        this.search = search;
        this.limit = limit;
        this.listener = listener;
    }

    @Nullable
    @Override
    protected Date getDateFromItem(Deck item) {
        return null;
    }

    @Override
    protected void userBindViewHolder(ViewHolder holder, int position) {
        final Deck deck = items.get(position).getItem();

        holder.name.setText(deck.getName());
        holder.author.setText(context.getString(R.string.byLowercase, deck.getAuthor().getUsername()));
        holder.nsfw.setVisibility(deck.isHasNsfwCards() ? View.VISIBLE : View.GONE);

        if (deck.getSampleCalls() != null && !deck.getSampleCalls().isEmpty()
                && deck.getSampleResponses() != null && !deck.getSampleResponses().isEmpty()) {
            Card exampleBlackCard = deck.getSampleCalls().get(random.nextInt(deck.getSampleCalls().size()));
            Card exampleWhiteCard = deck.getSampleResponses().get(random.nextInt(deck.getSampleResponses().size()));
            holder.example.setHtml(Utils.composeCardcastDeckSentence(exampleBlackCard, exampleWhiteCard));
            holder.example.setVisibility(View.VISIBLE);
        } else {
            holder.example.setVisibility(View.GONE);
        }

        holder.rating.setRating(deck.getRating()); // FIXME: Shouldn't be clickable
        holder.blackCards.setText(String.valueOf(deck.getCalls()));
        holder.whiteCards.setText(String.valueOf(deck.getResponses()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onDeckSelected(deck);
            }
        });

        CommonUtils.setCardTopMargin(context, holder);
    }

    @Override
    protected RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(parent);
    }

    @Override
    protected void moreContent(int page, final IContentProvider<Deck> provider) {
        cardcast.getDecks(search, limit, limit * page, new CardcastHelper.IDecks() {
            @Override
            public void onDone(CardcastHelper.Search search, Decks decks) {
                provider.onMoreContent(decks);
            }

            @Override
            public void onException(Exception ex) {
                provider.onFailed(ex);
            }
        });
    }

    public interface IAdapter {
        void onDeckSelected(Deck deck);
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
            super(inflater.inflate(R.layout.cardcast_deck_item, parent, false));

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
