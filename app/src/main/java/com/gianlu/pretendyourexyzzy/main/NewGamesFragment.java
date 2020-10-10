package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GamesList;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewGamesBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemNewGameBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class NewGamesFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private FragmentNewGamesBinding binding;
    private RegisteredPyx pyx;

    @NonNull
    public static NewGamesFragment get() {
        return new NewGamesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewGamesBinding.inflate(inflater, container, false);
        binding.gamesFragmentList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        return binding.getRoot();
    }

    private void gamesLoaded(@NonNull GamesList games) {
        GamesAdapter adapter = new GamesAdapter(games);
        binding.gamesFragmentList.setAdapter(adapter);

        if (Prefs.getBoolean(PK.FILTER_LOCKED_LOBBIES)) {
            adapter.setFilterOutLockedLobbies(true);
            binding.gamesFragmentFilterLocked.setImageResource(R.drawable.baseline_lock_open_24);
        } else {
            adapter.setFilterOutLockedLobbies(false);
            binding.gamesFragmentFilterLocked.setImageResource(R.drawable.outline_lock_24);
        }

        binding.gamesFragmentFilterLocked.setOnClickListener(v -> {
            boolean filter = !adapter.doesFilterOutLockedLobbies();
            adapter.setFilterOutLockedLobbies(filter);
            binding.gamesFragmentFilterLocked.setImageResource(filter ? R.drawable.baseline_lock_open_24 : R.drawable.outline_lock_24);
        });
    }

    private void failedLoadingGames(@NonNull Exception ex) {
        // TODO
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        this.pyx.request(PyxRequests.getGamesList())
                .addOnSuccessListener(this::gamesLoaded)
                .addOnFailureListener(this::failedLoadingGames);

        binding.gamesFragmentServer.setText(pyx.server.name);
    }

    @Override
    public void onPyxInvalid() {
        this.pyx = null;
    }

    private class GamesAdapter extends OrderedRecyclerViewAdapter<GamesAdapter.ViewHolder, Game, GamesFragment.SortBy, Game.Protection> {

        GamesAdapter(List<Game> games) {
            super(games, GamesFragment.SortBy.NAME);

            setHasStableIds(true);
        }

        void setFilterOutLockedLobbies(boolean filter) {
            if (filter) setFilters(Game.Protection.LOCKED);
            else setFilters();
        }

        boolean doesFilterOutLockedLobbies() {
            return filters.contains(Game.Protection.LOCKED);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        protected boolean matchQuery(@NonNull Game item, @Nullable String query) {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return objs.get(position).gid;
        }

        @Override
        public void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull Game game) {
            holder.binding.gameItemName.setText(game.host);
            CommonUtils.setText(holder.binding.gameItemPlayers, R.string.players, game.players.size(), game.options.playersLimit);
            CommonUtils.setText(holder.binding.gameItemSpectators, R.string.spectators, game.spectators.size(), game.options.spectatorsLimit);
            CommonUtils.setText(holder.binding.gameItemGoal, R.string.goal, game.options.scoreLimit);
            holder.binding.gameItemStatus.setImageResource(game.status.isStarted() ? R.drawable.baseline_casino_24 : R.drawable.baseline_hourglass_empty_24);

            if (game.hasPassword(false)) {
                holder.binding.gameItemLocked.setImageResource(R.drawable.outline_lock_24);
                CommonUtils.setImageTintColor(holder.binding.gameItemLocked, R.color.red);
            } else {
                holder.binding.gameItemLocked.setImageResource(R.drawable.baseline_lock_open_24);
                CommonUtils.setImageTintColor(holder.binding.gameItemLocked, R.color.green);
            }
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Game payload) {
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
        }

        @NonNull
        @Override
        public Comparator<Game> getComparatorFor(@NonNull GamesFragment.SortBy sorting) {
            switch (sorting) {
                default:
                case NAME:
                    return new Game.NameComparator();
                case AVAILABLE_PLAYERS:
                    return new Game.NumAvailablePlayersComparator();
                case AVAILABLE_SPECTATORS:
                    return new Game.NumAvailableSpectatorsComparator();
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemNewGameBinding binding;

            ViewHolder(@NonNull ViewGroup parent) {
                super(NewGamesFragment.this.getLayoutInflater().inflate(R.layout.item_new_game, parent, false));
                binding = ItemNewGameBinding.bind(itemView);
            }
        }
    }
}
