package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.main.ongoinggame.SensitiveGameData;

import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> implements SensitiveGameData.AdapterInterface {
    private final List<GameInfo.Player> players;
    private final Listener listener;
    private final LayoutInflater inflater;
    private RecyclerView view;

    public PlayersAdapter(Context context, List<GameInfo.Player> players, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.players = players;
        this.listener = listener;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final GameInfo.Player player = players.get(position);
        holder.name.setText(player.name);
        holder.score.setHtml(R.string.score, player.score);

        if (OverloadedApi.get().isOverloadedUser(player.name))
            CommonUtils.setTextColor(holder.name, R.color.appColorBright);
        else
            CommonUtils.setTextColorFromAttr(holder.name, android.R.attr.textColorPrimary);

        switch (player.status) {
            case HOST:
                holder.status.setImageResource(R.drawable.baseline_person_24);
                break;
            case IDLE:
                holder.status.setImageResource(R.drawable.baseline_done_all_24);
                break;
            case JUDGING:
            case JUDGE:
                holder.status.setImageResource(R.drawable.baseline_gavel_24);
                break;
            case PLAYING:
                holder.status.setImageResource(R.drawable.baseline_hourglass_empty_24);
                break;
            case WINNER:
                holder.status.setImageResource(R.drawable.baseline_star_24);
                break;
            case SPECTATOR:
                holder.status.setImageResource(R.drawable.baseline_remove_red_eye_24);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlayerSelected(player);
        });
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    @Override
    public void dispatchUpdate(@NonNull DiffUtil.DiffResult result) {
        result.dispatchUpdatesTo(this);
    }

    @Override
    public void clearPool() {
        if (view != null) view.getRecycledViewPool().clear();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        view = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        view = null;
    }

    public interface Listener {
        void onPlayerSelected(@NonNull GameInfo.Player player);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final SuperTextView score;
        final ImageView status;

        ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_player, parent, false));
            setIsRecyclable(true);

            name = itemView.findViewById(R.id.playerItem_name);
            score = itemView.findViewById(R.id.playerItem_score);
            status = itemView.findViewById(R.id.playerItem_status);
        }
    }
}
