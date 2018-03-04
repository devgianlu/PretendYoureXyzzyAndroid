package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> {
    private final List<GameInfo.Player> players;
    private final LayoutInflater inflater;

    public PlayersAdapter(Context context, List<GameInfo.Player> players) {
        this.inflater = LayoutInflater.from(context);
        this.players = new ArrayList<>(players);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return players.get(position).name.hashCode();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameInfo.Player player = players.get(position);
        holder.name.setText(player.name);
        holder.score.setHtml(R.string.score, player.score);

        switch (player.status) {
            case HOST:
                holder.status.setImageResource(R.drawable.ic_person_black_48dp);
                break;
            case IDLE:
                holder.status.setImageResource(R.drawable.ic_access_time_black_48dp);
                break;
            case JUDGING:
            case JUDGE:
                holder.status.setImageResource(R.drawable.ic_gavel_black_48dp);
                break;
            case PLAYING:
                holder.status.setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
                break;
            case WINNER:
                holder.status.setImageResource(R.drawable.ic_star_black_48dp);
                break;
            case SPECTATOR:
                holder.status.setImageResource(R.drawable.ic_remove_red_eye_black_48dp);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    public void notifyItemChanged(GameInfo.Player player) {
        int pos = players.indexOf(player);
        if (pos != -1) {
            players.set(pos, player);
            notifyItemChanged(pos);
        }
    }

    public void notifyDataSetChanged(List<GameInfo.Player> players) {
        this.players.clear();
        this.players.addAll(players);
        notifyDataSetChanged();
    }

    public void notifyItemRemoved(String nickname) {
        for (int i = 0; i < players.size(); i++) {
            if (Objects.equals(players.get(i).name, nickname)) {
                players.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void notifyItemInserted(GameInfo.Player player) {
        this.players.add(player);
        notifyItemInserted(players.size() - 1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final SuperTextView score;
        final ImageView status;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_player, parent, false));
            setIsRecyclable(true);

            name = itemView.findViewById(R.id.playerItem_name);
            score = itemView.findViewById(R.id.playerItem_score);
            status = itemView.findViewById(R.id.playerItem_status);
        }
    }
}
