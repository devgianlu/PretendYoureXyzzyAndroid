package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.commonutils.Adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Comparator;
import java.util.List;

public class GamesAdapter extends OrderedRecyclerViewAdapter<GamesAdapter.ViewHolder, Game, GamesAdapter.SortBy, Game.Protection> {
    private final Context context;
    private final IAdapter handler;
    private final LayoutInflater inflater;
    private final FirstLoadedPyx pyx;

    public GamesAdapter(Context context, List<Game> objs, FirstLoadedPyx pyx, boolean filterOutLockedLobbies, IAdapter handler) {
        super(objs, SortBy.NUM_PLAYERS);
        this.context = context;
        this.pyx = pyx;
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
    protected boolean matchQuery(@NonNull Game item, @Nullable String query) {
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Game game = objs.get(position);
        holder.name.setText(game.host);
        holder.players.setHtml(R.string.players, game.players.size(), game.options.playersLimit);
        holder.spectators.setHtml(R.string.spectators, game.spectators.size(), game.options.spectatorsLimit);
        holder.goal.setHtml(R.string.goal, game.options.scoreLimit);
        holder.locked.setImageResource(game.hasPassword ? R.drawable.ic_lock_outline_black_48dp : R.drawable.ic_lock_open_black_48dp);
        holder.status.setImageResource(game.status == Game.Status.LOBBY ? R.drawable.ic_hourglass_empty_black_48dp : R.drawable.ic_casino_black_48dp);
        holder.timerMultiplier.setHtml(R.string.timerMultiplier, game.options.timerMultiplier);
        holder.blankCards.setHtml(R.string.blankCards, game.options.blanksLimit);
        holder.cardsets.setHtml(R.string.cardSets, game.options.cardSets.isEmpty() ? "<i>none</i>" : CommonUtils.join(pyx.firstLoad().createCardSetNamesList(game.options.cardSets), ", "));

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

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtils.handleCollapseClick(holder.expand, holder.details);
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
        final ImageButton expand;
        final TextView name;
        final SuperTextView players;
        final SuperTextView spectators;
        final SuperTextView goal;
        final LinearLayout details;
        final SuperTextView cardsets;
        final SuperTextView timerMultiplier;
        final SuperTextView blankCards;

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
            expand = itemView.findViewById(R.id.gameItem_expand);
            details = itemView.findViewById(R.id.gameItem_details);
            cardsets = details.findViewById(R.id.gameItem_cardsets);
            timerMultiplier = details.findViewById(R.id.gameItem_timerMultiplier);
            blankCards = details.findViewById(R.id.gameItem_blankCards);
        }
    }
}
