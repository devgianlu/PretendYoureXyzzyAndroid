package com.gianlu.pretendyourexyzzy.Metrics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Date;
import java.util.List;

class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<SessionHistory.Game> games;

    GamesAdapter(@NonNull Context context, List<SessionHistory.Game> games, Listener listener) {
        this.games = games;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final SessionHistory.Game game = games.get(position);
        ((SuperTextView) holder.itemView).setHtml(R.string.gameStartedAt, CommonUtils.getFullVerbalDateFormatter().format(new Date(game.timestamp)));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onGameSelected(game);
        });
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    public interface Listener {
        void onGameSelected(@NonNull SessionHistory.Game game);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_metrics_game, parent, false));
        }
    }
}
