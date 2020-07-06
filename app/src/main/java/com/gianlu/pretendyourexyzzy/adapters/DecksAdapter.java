package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.Deck;

import java.util.Collections;
import java.util.List;

public class DecksAdapter extends RecyclerView.Adapter<DecksAdapter.ViewHolder> {
    private final List<Deck> decks;
    private final LayoutInflater inflater;
    private final Listener listener;

    public DecksAdapter(@NonNull Context context, List<Deck> decks, Listener listener) {
        this.decks = decks;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;

        setHasStableIds(true);
        listener.shouldUpdateItemCount(getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return decks.get(position).id;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Deck deck = decks.get(position);

        holder.name.setText(Html.fromHtml(deck.name));
        holder.whiteCards.setText(String.valueOf(deck.whiteCards));
        holder.blackCards.setText(String.valueOf(deck.blackCards));
        if (deck.watermark != null) {
            holder.watermark.setVisibility(View.VISIBLE);
            holder.watermark.setText(deck.watermark);
        } else {
            holder.watermark.setVisibility(View.GONE);
        }

        if (listener.canModifyCustomDecks()) {
            holder.remove.setVisibility(View.VISIBLE);
            holder.remove.setOnClickListener(v -> listener.removeDeck(deck));
        } else {
            holder.remove.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onDeckSelected(deck));
    }

    public void add(@NonNull Deck deck) {
        decks.add(0, deck);
        notifyItemInserted(0);
        listener.shouldUpdateItemCount(getItemCount());
    }

    public void remove(@NonNull Deck deck) {
        int index = decks.indexOf(deck);
        if (index != -1) {
            decks.remove(index);
            notifyItemRemoved(index);
            listener.shouldUpdateItemCount(getItemCount());
        }
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    @NonNull
    public List<Deck> getDecks() {
        return Collections.unmodifiableList(decks);
    }

    public interface Listener {
        void shouldUpdateItemCount(int count);

        void removeDeck(@NonNull Deck deck);

        void onDeckSelected(@NonNull Deck deck);

        boolean canModifyCustomDecks();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView whiteCards;
        final TextView blackCards;
        final TextView watermark;
        final ImageButton remove;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_cardset, parent, false));

            name = itemView.findViewById(R.id.cardSetItem_name);
            whiteCards = itemView.findViewById(R.id.cardSetItem_whiteCards);
            blackCards = itemView.findViewById(R.id.cardSetItem_blackCards);
            watermark = itemView.findViewById(R.id.cardSetItem_watermark);
            remove = itemView.findViewById(R.id.cardSetItem_remove);
        }
    }
}

