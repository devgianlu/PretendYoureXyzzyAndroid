package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<String> players;

    public PlayersAdapter(Context context, List<String> players) {
        this.players = players;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public PlayersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(PlayersAdapter.ViewHolder holder, int position) {
        holder.text.setText(players.get(position));
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.player_item, parent, false));
            text = (TextView) itemView;
        }
    }
}
