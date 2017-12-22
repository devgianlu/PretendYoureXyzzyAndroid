package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Sorting.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Comparator;
import java.util.List;

public class GamesAdapter extends OrderedRecyclerViewAdapter<GamesAdapter.ViewHolder, Game, GamesAdapter.SortBy, Game.Protection> {
    private final Context context;
    private final IAdapter handler;
    private final LayoutInflater inflater;

    public GamesAdapter(Context context, List<Game> objs, boolean filterOutLockedLobbies, IAdapter handler) {
        super(objs, SortBy.NUM_PLAYERS);
        this.context = context;
        this.handler = handler;
        this.inflater = LayoutInflater.from(context);
        setHasStableIds(true);
        setFilterOutLockedLobbies(filterOutLockedLobbies);
    }

    public void setFilterOutLockedLobbies(boolean filter) {
        if (filter) setFilters(Game.Protection.LOCKED);
        else setFilters();
    }

    @Override
    public long getItemId(int position) {
        return objs.get(position).gid;
    }

    @Nullable
    @Override
    protected RecyclerView getRecyclerView() {
        return handler != null ? handler.getRecyclerView() : null;
    }

    @Override
    protected boolean matchQuery(Game item, @Nullable String query) {
        return query == null || item.host.toLowerCase().contains(query.toLowerCase());
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, int position, @NonNull Game payload) {
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
    }

    @NonNull
    @Override
    public Comparator<Game> getComparatorFor(SortBy sorting) {
        switch (sorting) {
            case NAME:
                return new Game.NameComparator();
            default:
            case NUM_PLAYERS:
                return new Game.NumPlayersComparator();
            case NUM_SPECTATORS:
                return new Game.NumSpectatorsComparator();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Game game = objs.get(position);
        holder.name.setText(game.host);
        holder.players.setHtml(R.string.players, game.players.size(), game.options.playersLimit);
        holder.spectators.setHtml(R.string.spectators, game.spectators.size(), game.options.spectatorsLimit);
        holder.goal.setHtml(R.string.goal, game.options.scoreLimit);
        holder.locked.setImageResource(game.hasPassword ? R.drawable.ic_lock_outline_black_48dp : R.drawable.ic_lock_open_black_48dp);
        holder.status.setImageResource(game.status == Game.Status.LOBBY ? R.drawable.ic_hourglass_empty_black_48dp : R.drawable.ic_casino_black_48dp);

        holder.spectate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.spectateGame(game);
            }
        });

        holder.join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.joinGame(game);
            }
        });

        CommonUtils.setRecyclerViewTopMargin(context, holder);
    }

    public List<Game> getGames() {
        return objs;
    }

    public enum SortBy {
        NAME,
        NUM_PLAYERS,
        NUM_SPECTATORS
    }

    public interface IAdapter {
        @Nullable
        RecyclerView getRecyclerView();

        void spectateGame(Game game);

        void joinGame(Game game);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView locked;
        public final ImageView status;
        public final Button spectate;
        public final Button join;
        final TextView name;
        final SuperTextView players;
        final SuperTextView spectators;
        final SuperTextView goal;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_game, parent, false));

            name = itemView.findViewById(R.id.gameItem_name);
            status = itemView.findViewById(R.id.gameItem_status);
            players = itemView.findViewById(R.id.gameItem_players);
            locked = itemView.findViewById(R.id.gameItem_locked);
            spectators = itemView.findViewById(R.id.gameItem_spectators);
            goal = itemView.findViewById(R.id.gameItem_goal);
            spectate = itemView.findViewById(R.id.gameItem_spectate);
            join = itemView.findViewById(R.id.gameItem_join);
        }
    }
}
