package com.gianlu.pretendyourexyzzy.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.starred.StarredDecksManager;

import java.util.List;

public class StarredDecksAdapter extends RecyclerView.Adapter<StarredDecksAdapter.ViewHolder> {
    private final List<StarredDecksManager.StarredDeck> decks;
    private final LayoutInflater inflater;
    private final Listener listener;

    public StarredDecksAdapter(@NonNull Context context, List<StarredDecksManager.StarredDeck> decks, Listener listener) {
        this.decks = decks;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final StarredDecksManager.StarredDeck deck = decks.get(position);

        holder.name.setText(deck.name);
        holder.code.setText(deck.code);
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onDeckSelected(deck);
        });

        CommonUtils.setRecyclerViewTopMargin(holder);
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    public interface Listener {
        void onDeckSelected(@NonNull StarredDecksManager.StarredDeck deck);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView code;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_starred_deck, parent, false));

            name = itemView.findViewById(R.id.starredDeckItem_name);
            code = itemView.findViewById(R.id.starredDeckItem_code);
        }
    }
}
