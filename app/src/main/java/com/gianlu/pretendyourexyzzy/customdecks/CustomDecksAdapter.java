package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.FloatingCustomDeck;

import java.util.List;

public final class CustomDecksAdapter extends RecyclerView.Adapter<CustomDecksAdapter.ViewHolder> {
    private final List<? extends FloatingCustomDeck> decks;
    private final Listener listener;
    private final LayoutInflater inflater;

    public CustomDecksAdapter(@NonNull Context context, @NonNull List<? extends FloatingCustomDeck> decks, @NonNull Listener listener) {
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
        FloatingCustomDeck deck = decks.get(position);
        holder.name.setText(deck.name);
        holder.watermark.setText(deck.watermark);
        holder.itemView.setOnClickListener(view -> listener.onCustomDeckSelected(deck));
        CommonUtils.setRecyclerViewTopMargin(holder);
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    public interface Listener {
        void onCustomDeckSelected(@NonNull FloatingCustomDeck deck);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView watermark;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_custom_deck, parent, false));

            name = itemView.findViewById(R.id.customDeckItem_name);
            watermark = itemView.findViewById(R.id.customDeckItem_watermark);
        }
    }
}
