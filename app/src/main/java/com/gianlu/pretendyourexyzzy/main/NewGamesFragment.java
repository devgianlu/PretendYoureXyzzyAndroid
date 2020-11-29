package com.gianlu.pretendyourexyzzy.main;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GamesList;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewGamesBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemNewGameBinding;
import com.gianlu.pretendyourexyzzy.dialogs.ChangeServerDialog;
import com.gianlu.pretendyourexyzzy.dialogs.NewChatDialog;
import com.gianlu.pretendyourexyzzy.game.GameActivity;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class NewGamesFragment extends NewMainActivity.ChildFragment implements Pyx.OnEventListener {
    private static final String TAG = NewGamesFragment.class.getSimpleName();
    private FragmentNewGamesBinding binding;
    private RegisteredPyx pyx;
    private GamesAdapter adapter;

    @NonNull
    public static NewGamesFragment get() {
        return new NewGamesFragment();
    }

    private void setGamesStatus(boolean loading, boolean error, boolean empty) {
        if (loading) {
            binding.gamesFragmentList.setAdapter(null);
            binding.gamesFragmentListEmpty.setVisibility(View.GONE);
            binding.gamesFragmentListError.setVisibility(View.GONE);
            binding.gamesFragmentListLoading.setVisibility(View.VISIBLE);
            binding.gamesFragmentListLoading.showShimmer(true);

            binding.gamesFragmentSwipeRefresh.setEnabled(false);
        } else if (error) {
            binding.gamesFragmentList.setAdapter(null);
            binding.gamesFragmentListEmpty.setVisibility(View.GONE);
            binding.gamesFragmentListError.setVisibility(View.VISIBLE);
            binding.gamesFragmentListLoading.setVisibility(View.GONE);
            binding.gamesFragmentListLoading.hideShimmer();

            binding.gamesFragmentSwipeRefresh.setEnabled(true);
        } else if (empty) {
            binding.gamesFragmentListEmpty.setVisibility(View.VISIBLE);
            binding.gamesFragmentListError.setVisibility(View.GONE);
            binding.gamesFragmentListLoading.setVisibility(View.GONE);
            binding.gamesFragmentListLoading.hideShimmer();

            binding.gamesFragmentSwipeRefresh.setEnabled(true);
        } else {
            binding.gamesFragmentListEmpty.setVisibility(View.GONE);
            binding.gamesFragmentListError.setVisibility(View.GONE);
            binding.gamesFragmentListLoading.setVisibility(View.GONE);
            binding.gamesFragmentListLoading.hideShimmer();

            binding.gamesFragmentSwipeRefresh.setEnabled(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewGamesBinding.inflate(inflater, container, false);

        binding.gamesFragmentList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        binding.gamesFragmentSwipeRefresh.setColorSchemeResources(R.color.appColor_500);
        binding.gamesFragmentSwipeRefresh.setOnRefreshListener(() -> {
            if (pyx == null) return;

            setGamesStatus(true, false, false);
            pyx.request(PyxRequests.getGamesList())
                    .addOnSuccessListener(this::gamesLoaded)
                    .addOnFailureListener(this::failedLoadingGames);

            binding.gamesFragmentSwipeRefresh.setRefreshing(false);
        });

        binding.gamesFragmentChangeServer.setOnClickListener(v -> ChangeServerDialog.get().show(requireFragmentManager(), null));

        binding.gamesFragmentFilterLocked.setOnClickListener(v -> {
            boolean filter = !Prefs.getBoolean(PK.FILTER_LOCKED_LOBBIES);
            Prefs.putBoolean(PK.FILTER_LOCKED_LOBBIES, filter);
            binding.gamesFragmentFilterLocked.setImageResource(filter ? R.drawable.baseline_lock_open_24 : R.drawable.outline_lock_24);

            if (adapter != null) adapter.setFilterOutLockedLobbies(filter);
        });
        binding.gamesFragmentFilterStatus.setOnClickListener(v -> {
            String filter = Prefs.getString(PK.FILTER_GAME_STATUS);
            if (filter.equals("any")) filter = "in_progress";
            else if (filter.equals("in_progress")) filter = "lobby";
            else filter = "any";
            Prefs.putString(PK.FILTER_GAME_STATUS, filter);
            updateGameStatusFilter(filter);
        });

        binding.gamesFragmentMenu.setOnClickListener((v) -> showPopupMenu());
        binding.gamesFragmentCreateGame.setOnClickListener(v -> pyx.request(PyxRequests.createGame())
                .addOnSuccessListener((gamePermalink) -> startActivity(GameActivity.gameIntent(requireContext(), gamePermalink)))
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed creating game.", ex);
                    showToast(Toaster.build().message(R.string.failedCreatingGame));
                }));

        setKeepScreenOn(Prefs.getBoolean(PK.KEEP_SCREEN_ON));

        setGamesStatus(true, false, false);

        return binding.getRoot();
    }

    private void updateGameStatusFilter(@NonNull String filter) {
        switch (filter) {
            default:
            case "any":
                binding.gamesFragmentFilterStatus.setImageResource(R.drawable.baseline_casino_hourglass_24);
                CommonUtils.setPaddingDip(binding.gamesFragmentFilterStatus, 0);
                if (adapter != null) {
                    adapter.removeFilter(Game.Filters.IN_PROGRESS);
                    adapter.removeFilter(Game.Filters.LOBBY);
                }
                break;
            case "in_progress":
                binding.gamesFragmentFilterStatus.setImageResource(R.drawable.baseline_casino_24);
                CommonUtils.setPaddingDip(binding.gamesFragmentFilterStatus, 4);
                if (adapter != null) {
                    adapter.removeFilter(Game.Filters.IN_PROGRESS);
                    adapter.addFilter(Game.Filters.LOBBY);
                }
                break;
            case "lobby":
                binding.gamesFragmentFilterStatus.setImageResource(R.drawable.baseline_hourglass_empty_24);
                CommonUtils.setPaddingDip(binding.gamesFragmentFilterStatus, 4);
                if (adapter != null) {
                    adapter.addFilter(Game.Filters.IN_PROGRESS);
                    adapter.removeFilter(Game.Filters.LOBBY);
                }
                break;
        }
    }

    private void gamesLoaded(@NonNull GamesList games) {
        adapter = new GamesAdapter(games);
        binding.gamesFragmentList.setAdapter(adapter);

        if (Prefs.getBoolean(PK.FILTER_LOCKED_LOBBIES)) {
            adapter.setFilterOutLockedLobbies(true);
            binding.gamesFragmentFilterLocked.setImageResource(R.drawable.baseline_lock_open_24);
        } else {
            adapter.setFilterOutLockedLobbies(false);
            binding.gamesFragmentFilterLocked.setImageResource(R.drawable.outline_lock_24);
        }

        updateGameStatusFilter(Prefs.getString(PK.FILTER_GAME_STATUS));
    }

    private void failedLoadingGames(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading games.", ex);
        setGamesStatus(false, true, false);
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;
        this.pyx.polling().addListener(this);

        this.pyx.request(PyxRequests.getGamesList())
                .addOnSuccessListener(this::gamesLoaded)
                .addOnFailureListener(this::failedLoadingGames);

        binding.gamesFragmentCreateGame.setEnabled(true);
        binding.gamesFragmentSwipeRefresh.setEnabled(true);
        binding.gamesFragmentChangeServer.setEnabled(true);
        binding.gamesFragmentServer.setText(pyx.server.name);
        binding.gamesFragmentServerLoading.hideShimmer();
        binding.gamesFragmentServerError.setVisibility(View.GONE);
        setServerBoxTint(null);

        if (pyx.config().gameChatEnabled()) {
            binding.gamesFragmentChat.setVisibility(View.VISIBLE);
            binding.gamesFragmentChat.setOnClickListener(v -> NewChatDialog.getGlobal().show(getChildFragmentManager(), null));
        } else {
            binding.gamesFragmentChat.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPyxInvalid(@Nullable Exception ex) {
        if (pyx != null) pyx.polling().removeListener(this);
        this.pyx = null;

        if (binding != null) {
            setGamesStatus(ex == null, ex != null, false);

            Pyx.Server lastServer = Pyx.Server.lastServerNoThrow();
            binding.gamesFragmentServer.setText(lastServer == null ? "..." : lastServer.name);

            binding.gamesFragmentCreateGame.setEnabled(false);
            binding.gamesFragmentSwipeRefresh.setEnabled(false);
            binding.gamesFragmentChat.setVisibility(View.GONE);

            if (ex == null) { // Loading
                binding.gamesFragmentChangeServer.setEnabled(false);
                binding.gamesFragmentServer.setText(null);
                binding.gamesFragmentServerLoading.showShimmer(true);
                binding.gamesFragmentServerError.setVisibility(View.GONE);
                setServerBoxTint(null);
            } else { // Actual error
                binding.gamesFragmentChangeServer.setEnabled(true);
                binding.gamesFragmentServerLoading.hideShimmer();

                int errorRes;
                if (ex instanceof PyxException) errorRes = ((PyxException) ex).getPyxMessage();
                else errorRes = R.string.failedLoading_changeServerRetry;

                binding.gamesFragmentServerError.setVisibility(View.VISIBLE);
                binding.gamesFragmentServerError.setText(errorRes);
                setServerBoxTint(Color.rgb(255, 204, 204));
            }
        }
    }

    private void setServerBoxTint(@Nullable Integer color) {
        if (binding == null) return;

        if (color == null)
            binding.gamesFragmentServerLoading.getChildAt(0).setBackgroundTintList(null);
        else
            binding.gamesFragmentServerLoading.getChildAt(0).setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    public boolean goBack() {
        return false;
    }

    private void showPopupMenu() {
        PopupMenu menu = new PopupMenu(requireContext(), binding.gamesFragmentMenu);
        menu.inflate(R.menu.new_games);
        menu.getMenu().findItem(R.id.games_keepScreenOn).setChecked(Prefs.getBoolean(PK.KEEP_SCREEN_ON));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.games_keepScreenOn) {
                boolean on = !item.isChecked();
                item.setChecked(on);
                setKeepScreenOn(on);
                return true;
            }

            return false;
        });
        menu.show();
    }

    private void setKeepScreenOn(boolean on) {
        Prefs.putBoolean(PK.KEEP_SCREEN_ON, on);

        Window window = requireActivity().getWindow();
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pyx != null) pyx.polling().removeListener(this);
        this.pyx = null;
    }

    @Override
    public void onPollMessage(@NonNull PollMessage message) {
        if (message.event == PollMessage.Event.GAME_LIST_REFRESH) {
            pyx.request(PyxRequests.getGamesList())
                    .addOnSuccessListener((games) -> {
                        if (adapter != null) adapter.itemsChanged(games);
                    })
                    .addOnFailureListener((ex) -> Log.e(TAG, "Failed refreshing game list."));
        }
    }

    public enum SortBy {
        NAME, AVAILABLE_PLAYERS, AVAILABLE_SPECTATORS
    }

    private class GamesAdapter extends OrderedRecyclerViewAdapter<GamesAdapter.ViewHolder, Game, SortBy, Game.Filters> {
        GamesAdapter(List<Game> games) {
            super(games, SortBy.AVAILABLE_PLAYERS);
            setHasStableIds(true);
        }

        void setFilterOutLockedLobbies(boolean filter) {
            if (filter) addFilter(Game.Filters.LOCKED);
            else removeFilter(Game.Filters.LOCKED);
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

            holder.binding.gameItemSpectate.setOnClickListener(v -> askPassword(game)
                    .continueWithTask(task -> pyx.request(PyxRequests.spectateGame(game.gid, task.getResult())))
                    .addOnSuccessListener((gamePermalink) -> {
                        AnalyticsApplication.sendAnalytics(Utils.ACTION_JOIN_GAME);
                        startActivity(GameActivity.gameIntent(requireContext(), gamePermalink));
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed spectating game.", ex);
                        showToast(Toaster.build().message(R.string.failedSpectating).extra(game.gid));
                    }));

            holder.binding.gameItemJoin.setOnClickListener(v -> askPassword(game)
                    .continueWithTask(task -> pyx.request(PyxRequests.joinGame(game.gid, task.getResult())))
                    .addOnSuccessListener((gamePermalink) -> {
                        startActivity(GameActivity.gameIntent(requireContext(), gamePermalink));
                        AnalyticsApplication.sendAnalytics(Utils.ACTION_JOIN_GAME);
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed joining game.", ex);
                        showToast(Toaster.build().message(R.string.failedJoiningGame).extra(game.gid));
                    }));
        }

        @NotNull
        private Task<String> askPassword(@NotNull Game game) {
            if (game.hasPassword(false)) {
                EditText password = new EditText(requireContext());
                password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                CancellationTokenSource cancellationSource = new CancellationTokenSource();
                TaskCompletionSource<String> completionSource = new TaskCompletionSource<>(cancellationSource.getToken());

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setTitle(R.string.gamePassword)
                        .setView(password)
                        .setNegativeButton(R.string.cancel, (dialog, which) -> cancellationSource.cancel())
                        .setPositiveButton(R.string.submit, (dialog, which) -> completionSource.setResult(password.getText().toString()));

                showDialog(builder);
                return completionSource.getTask();
            } else {
                return Tasks.forResult(null);
            }
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Game payload) {
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            setGamesStatus(false, false, count == 0);
        }

        @NonNull
        @Override
        public Comparator<Game> getComparatorFor(@NonNull SortBy sorting) {
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
