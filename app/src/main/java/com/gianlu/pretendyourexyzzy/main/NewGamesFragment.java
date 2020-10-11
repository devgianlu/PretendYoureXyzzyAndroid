package com.gianlu.pretendyourexyzzy.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.activities.ManageServersActivity;
import com.gianlu.pretendyourexyzzy.adapters.ServersAdapter;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GamesList;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewGamesBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemNewGameBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class NewGamesFragment extends FragmentWithDialog implements NewMainActivity.MainFragment {
    private FragmentNewGamesBinding binding;
    private RegisteredPyx pyx;
    private NewMainActivity parent;

    @NonNull
    public static NewGamesFragment get() {
        return new NewGamesFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof NewMainActivity)
            parent = (NewMainActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        parent = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewGamesBinding.inflate(inflater, container, false);
        binding.gamesFragmentList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        binding.gamesFragmentChangeServer.setOnClickListener(v -> changeServerDialog(true));

        return binding.getRoot();
    }

    private void changeServerDialog(boolean dismissible) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.changeServer)
                .setCancelable(dismissible)
                .setNeutralButton(R.string.manage, (dialog, which) -> {
                    startActivity(new Intent(requireContext(), ManageServersActivity.class));
                    dialog.dismiss();
                });

        if (dismissible)
            builder.setNegativeButton(android.R.string.cancel, null);

        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_dialog_manage_servers, null, false);
        builder.setView(layout);

        RecyclerMessageView rmv = layout.findViewById(R.id.manageServers_list);
        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        rmv.loadListData(new ServersAdapter(requireContext(), Pyx.Server.loadAllServers(), new ServersAdapter.Listener() {
            @Override
            public void shouldUpdateItemCount(int count) {
            }

            @Override
            public void serverSelected(@NonNull Pyx.Server server) {
                if (parent != null) parent.changeServer(server);
                dismissDialog();
            }
        }));

        showDialog(builder);
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

        // TODO: Clear layout
    }

    private class GamesAdapter extends OrderedRecyclerViewAdapter<GamesAdapter.ViewHolder, Game, GamesFragment.SortBy, Game.Protection> {

        GamesAdapter(List<Game> games) {
            super(games, GamesFragment.SortBy.AVAILABLE_PLAYERS);

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
            CommonUtils.setText(holder.binding.gameItemBlankCards, R.string.blankCards, game.options.blanksLimit);
            CommonUtils.setText(holder.binding.gameItemTimerMultiplier, R.string.timerMultiplier, game.options.timerMultiplier);
            holder.binding.gameItemStatus.setImageResource(game.status.isStarted() ? R.drawable.baseline_casino_24 : R.drawable.baseline_hourglass_empty_24);

            if (game.hasPassword(false)) {
                holder.binding.gameItemLocked.setImageResource(R.drawable.outline_lock_24);
                CommonUtils.setImageTintColor(holder.binding.gameItemLocked, R.color.red);
            } else {
                holder.binding.gameItemLocked.setImageResource(R.drawable.baseline_lock_open_24);
                CommonUtils.setImageTintColor(holder.binding.gameItemLocked, R.color.green);
            }

            if (pyx != null) {
                List<String> deckNames = new LinkedList<>();
                for (Deck d : game.customDecks) deckNames.add("<i>" + d.name + "</i>");
                for (int id : game.options.cardSets) deckNames.add(pyx.firstLoad().cardSetName(id));
                holder.binding.gameItemCardsets.setHtml(R.string.cardSets, deckNames.isEmpty() ? "<i>none</i>" : CommonUtils.join(deckNames, ", "));
                holder.binding.gameItemCardsets.setVisibility(View.VISIBLE);
            } else {
                holder.binding.gameItemCardsets.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (CommonUtils.isExpanded(holder.binding.gameItemDetails))
                    CommonUtils.collapse(holder.binding.gameItemDetails, null);
                else
                    CommonUtils.expand(holder.binding.gameItemDetails, null);
            });

            holder.binding.gameItemSpectate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Spectate game
                }
            });

            holder.binding.gameItemJoin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Join game
                }
            });
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Game payload) {
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            // TODO: No games imagery
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
