package com.gianlu.pretendyourexyzzy.metrics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.cards.NewGameCardView;

import java.util.List;

class RoundsAdapter extends RecyclerView.Adapter<RoundsAdapter.ViewHolder> {
    private final Listener listener;
    private final LayoutInflater inflater;
    private final List<SimpleRound> rounds;

    RoundsAdapter(@NonNull Context context, List<SimpleRound> rounds, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.rounds = rounds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SimpleRound round = rounds.get(position);
        holder.card.setCard(round.blackCard);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRoundSelected(round);
        });
    }

    @Override
    public int getItemCount() {
        return rounds.size();
    }

    public interface Listener {
        void onRoundSelected(@NonNull SimpleRound round);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final NewGameCardView card;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_grid_card, parent, false));
            card = (NewGameCardView) ((ViewGroup) itemView).getChildAt(0);
        }
    }
}
