package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public final class CustomDecksAdapter extends RecyclerView.Adapter<CustomDecksAdapter.ViewHolder> {
    private final List<? extends BasicCustomDeck> decks;
    private final Listener listener;
    private final LayoutInflater inflater;

    public CustomDecksAdapter(@NonNull Context context, @NonNull List<? extends BasicCustomDeck> decks, @NonNull Listener listener) {
        this.decks = decks;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return decks.get(position).hashCode();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BasicCustomDeck deck = decks.get(position);
        holder.name.setText(deck.name);
        holder.watermark.setText(deck.watermark);
        holder.itemView.setOnClickListener(view -> listener.onCustomDeckSelected(deck));
        CommonUtils.setRecyclerViewTopMargin(holder);

        if (deck.owner == null || deck.owner.equals(OverloadedApi.get().username())) {
            holder.owner.setVisibility(View.GONE);
        } else {
            holder.owner.setVisibility(View.VISIBLE);
            CommonUtils.setText(holder.owner, R.string.deckBy, deck.owner);
        }

        int whiteCards = deck.whiteCardsCount();
        int blackCards = deck.blackCardsCount();
        if (whiteCards != -1 && blackCards != -1)
            CommonUtils.setText(holder.cards, R.string.cardsCountBlackWhite, blackCards, whiteCards);
        else
            CommonUtils.setText(holder.cards, R.string.cardsCount, deck.cardsCount());
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    public interface Listener {
        void onCustomDeckSelected(@NonNull BasicCustomDeck deck);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView watermark;
        final TextView cards;
        final TextView owner;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_custom_deck, parent, false));

            name = itemView.findViewById(R.id.customDeckItem_name);
            watermark = itemView.findViewById(R.id.customDeckItem_watermark);
            cards = itemView.findViewById(R.id.customDeckItem_cards);
            owner = itemView.findViewById(R.id.customDeckItem_owner);
        }
    }
}
