package com.gianlu.pretendyourexyzzy.Metrics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

class RoundsAdapter extends RecyclerView.Adapter<RoundsAdapter.ViewHolder> {
    private final Listener listener;
    private final LayoutInflater inflater;
    private final List<SimpleRound> rounds;

    RoundsAdapter(Context context, List<SimpleRound> rounds, Listener listener) {
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
        final SimpleRound round = rounds.get(position);
        ((GameCardView) holder.itemView).setCard(round.blackCard);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRoundSelected(round);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rounds.size();
    }

    public interface Listener {
        void onRoundSelected(@NonNull SimpleRound round);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_metrics_round, parent, false));
        }
    }
}
