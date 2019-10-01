package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.Adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Comparator;
import java.util.List;

public class GamesAdapter extends OrderedRecyclerViewAdapter<GamesAdapter.ViewHolder, Game, GamesAdapter.SortBy, Game.Protection> {
    private final Listener listener;
    private final LayoutInflater inflater;
    private final FirstLoadedPyx pyx;

    public GamesAdapter(Context context, List<Game> objs, FirstLoadedPyx pyx, boolean filterOutLockedLobbies, Listener listener) {
        super(objs, SortBy.NUM_PLAYERS);
        this.pyx = pyx;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);

        setHasStableIds(true);
        setFilterOutLockedLobbies(filterOutLockedLobbies);
    }

    public void setFilterOutLockedLobbies(boolean filter) {
        if (filter) setFilters(Game.Protection.LOCKED);
        else setFilters();
    }

    public boolean doesFilterOutLockedLobbies() {
        return filters.contains(Game.Protection.LOCKED);
    }

    @Override
    public long getItemId(int position) {
        return objs.get(position).gid;
    }

    @Override
    protected boolean matchQuery(@NonNull Game item, @Nullable String query) {
        return query == null || item.host.toLowerCase().contains(query.toLowerCase());
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (listener != null) listener.onItemCountUpdated(count);
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
    public void onSetupViewHolder(@NonNull final ViewHolder holder, int position, @NonNull final Game game) {
        holder.name.setText(game.host);
        holder.players.setHtml(R.string.players, game.players.size(), game.options.playersLimit);
        holder.spectators.setHtml(R.string.spectators, game.spectators.size(), game.options.spectatorsLimit);
        holder.goal.setHtml(R.string.goal, game.options.scoreLimit);
        holder.locked.setImageResource(game.hasPassword(false) ? R.drawable.outline_lock_24 : R.drawable.baseline_lock_open_24);
        CommonUtils.setImageTintColor(holder.locked, game.hasPassword(false) ? R.color.red : R.color.green);
        holder.status.setImageResource(game.status == Game.Status.LOBBY ? R.drawable.baseline_hourglass_empty_24 : R.drawable.baseline_casino_24);
        holder.timerMultiplier.setHtml(R.string.timerMultiplier, game.options.timerMultiplier);
        holder.blankCards.setHtml(R.string.blankCards, game.options.blanksLimit);
        holder.cardsets.setHtml(R.string.cardSets, game.options.cardSets.isEmpty() ? "<i>none</i>" : CommonUtils.join(pyx.firstLoad().createCardSetNamesList(game.options.cardSets), ", "));

        holder.spectate.setOnClickListener(v -> {
            if (listener != null) listener.spectateGame(game);
        });

        holder.join.setOnClickListener(v -> {
            if (listener != null) listener.joinGame(game);
        });

        holder.expand.setOnClickListener(v -> CommonUtils.handleCollapseClick(holder.expand, holder.details));

        CommonUtils.setRecyclerViewTopMargin(holder);
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Game payload) {
    }

    @NonNull
    public List<Game> getGames() {
        return originalObjs;
    }

    @NonNull
    public List<Game> getVisibleGames() {
        return objs;
    }

    public enum SortBy {
        NAME,
        NUM_PLAYERS,
        NUM_SPECTATORS
    }

    public interface Listener {
        void spectateGame(@NonNull Game game);

        void joinGame(@NonNull Game game);

        void onItemCountUpdated(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView locked;
        public final ImageView status;
        public final Button spectate;
        public final Button join;
        public final ImageButton expand;
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
