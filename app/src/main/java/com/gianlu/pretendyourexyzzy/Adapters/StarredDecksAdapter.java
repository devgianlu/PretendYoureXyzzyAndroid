package com.gianlu.pretendyourexyzzy.Adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Starred.StarredDecksManager;

import java.util.List;

public class StarredDecksAdapter extends RecyclerView.Adapter<StarredDecksAdapter.ViewHolder> {
    private final Context context;
    private final List<StarredDecksManager.StarredDeck> decks;
    private final LayoutInflater inflater;
    private final Listener listener;

    public StarredDecksAdapter(Context context, List<StarredDecksManager.StarredDeck> decks, Listener listener) {
        this.context = context;
        this.decks = decks;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return decks.get(position).code.hashCode();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final StarredDecksManager.StarredDeck deck = decks.get(position);

        holder.name.setText(deck.name);
        holder.code.setText(deck.code);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onDeckSelected(deck);
            }
        });

        CommonUtils.setRecyclerViewTopMargin(context, holder);
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    public interface Listener {
        void onDeckSelected(StarredDecksManager.StarredDeck deck);
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
