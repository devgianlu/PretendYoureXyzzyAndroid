package com.gianlu.pretendyourexyzzy.game;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.glide.GlideUtils;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.UnknownCard;
import com.gianlu.pretendyourexyzzy.cards.NewCardsGroupView;
import com.gianlu.pretendyourexyzzy.databinding.ActivityNewGameBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemNewPlayerBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

/**
 * Handles all the UI/layout operations.
 */
public class GameUi {
    private final Context context;
    private final com.gianlu.pretendyourexyzzy.databinding.ActivityNewGameBinding binding;
    private final Timer timer = new Timer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final boolean customDecksEnabled;
    private Listener listener = null;
    private CountdownTask countdownTask;
    private PlayersAdapter playersAdapter;
    private CardsAdapter tableAdapter;
    private CardsAdapter handAdapter;
    private BaseCard blackCard;

    public GameUi(@NotNull Context context, @NotNull ActivityNewGameBinding binding, @NonNull RegisteredPyx pyx) {
        this.context = context;
        this.binding = binding;
        this.customDecksEnabled = pyx.config().customDecksEnabled() || pyx.config().crCastEnabled();

        binding.gameActivityWhiteCards.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        binding.gameActivityPlayers.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        binding.gameActivityLobbyPlayers.setLayoutManager(new GridLayoutManager(context, 4, RecyclerView.VERTICAL, false));

        binding.gameActivityStart.setOnClickListener(v -> {
            if (listener != null) listener.startGame();
        });
        binding.gameActivityOptions.setOnClickListener(v -> {
            if (listener != null) listener.showOptions(false);
        });
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /**
     * Create the adapters and attach the {@link PlayersAdapter} to {@link GameData}.
     *
     * @param gameData The {@link GameData} instance
     */
    public void setup(@NonNull GameData gameData) {
        playersAdapter = new PlayersAdapter(gameData.players /* Pass this directly */);
        gameData.playersAdapter = playersAdapter;

        tableAdapter = new CardsAdapter();
        handAdapter = new CardsAdapter();
    }

    public void setInstructions(@StringRes int textRes, Object... args) {
        binding.gameActivityStateText.setText(context.getString(textRes, args));
    }

    public void startGameVisible(boolean visible) {
        binding.gameActivityStart.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.gameActivityOptions.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    //region Counter
    public void countFrom(int ms) {
        if (countdownTask != null) countdownTask.cancel();

        binding.gameActivityCounter.setVisibility(View.VISIBLE);
        if (ms < 2147000) {
            countdownTask = new CountdownTask(ms / 1000);
            timer.scheduleAtFixedRate(countdownTask, 0, 1000);
        } else {
            binding.gameActivityCounter.setText("∞\u00A0");
        }
    }

    public void resetTimer() {
        if (countdownTask != null) countdownTask.cancel();
        binding.gameActivityCounter.setVisibility(View.GONE);
    }
    //endregion

    //region Black card
    public void setBlackCard(@Nullable BaseCard card) {
        this.blackCard = card;

        if (card != null) {
            binding.gameActivityBlackCardText.setHtml(card.textUnescaped());
            binding.gameActivityBlackCardPick.setHtml(R.string.numPick, card.numPick());
            binding.gameActivityBlackCardWatermark.setText(card.watermark());
        } else {
            binding.gameActivityBlackCardText.setHtml(null);
            binding.gameActivityBlackCardPick.setHtml(null);
            binding.gameActivityBlackCardWatermark.setText(null);
        }
    }

    @Nullable
    public BaseCard blackCard() {
        return blackCard;
    }
    //endregion

    //region White cards
    public void clearHand() {
        handAdapter.clear();
        adjustBlackCard();
    }

    public void showHand(boolean selectable) {
        handAdapter.setSelectable(selectable);
        binding.gameActivityWhiteCards.swapAdapter(handAdapter, true);
        adjustBlackCard();
    }

    public void setHand(@NotNull List<BaseCard> cards) {
        handAdapter.setSingles(cards);
        adjustBlackCard();
    }

    public void addHand(@NotNull List<BaseCard> cards) {
        handAdapter.addSingles(cards);
        adjustBlackCard();
    }

    public void removeHand(@NotNull BaseCard card) {
        handAdapter.removeWithGroup(card);
        adjustBlackCard();
    }

    public void clearTable() {
        tableAdapter.clear();
        adjustBlackCard();
    }

    public void showTable(boolean selectable) {
        tableAdapter.setSelectable(selectable);
        binding.gameActivityWhiteCards.swapAdapter(tableAdapter, true);
        adjustBlackCard();
    }

    public void setTable(@NotNull List<CardsGroup> cards, BaseCard blackCard) {
        if (blackCard != null) {
            for (CardsGroup group : cards) {
                if (group.isUnknwon()) {
                    for (int i = 1; i < blackCard.numPick(); i++)
                        group.add(new UnknownCard());
                }
            }
        }

        tableAdapter.setGroups(cards);
        adjustBlackCard();
    }

    public void addTable(@NotNull GameCard card, BaseCard blackCard) {
        if (blackCard != null && blackCard.numPick() > 1) {
            List<BaseCard> cards = tableAdapter.findAndRemoveFaceUpCards();
            cards.add(card);
            tableAdapter.addSingles(cards);
        } else {
            tableAdapter.addSingles(Collections.singletonList(card));
        }

        adjustBlackCard();
    }

    public void addBlankCardTable() {
        if (blackCard != null) tableAdapter.addGroup(CardsGroup.unknown(blackCard.numPick()));
        adjustBlackCard();
    }

    public void notifyWinnerCard(int winnerCardId) {
        if (tableAdapter != null) tableAdapter.notifyWinnerCard(winnerCardId);
        adjustBlackCard();
    }

    private void adjustBlackCard() {
        RecyclerView.Adapter<?> adapter = binding.gameActivityWhiteCards.getAdapter();
        if (adapter == null)
            return;

        binding.gameActivityWhiteCards.post(() -> {
            int margin;
            if (adapter.getItemCount() > 0)
                margin = binding.gameActivityWhiteCards.getHeight()
                        - ((FrameLayout.LayoutParams) binding.gameActivityBlackCard.getLayoutParams()).bottomMargin
                        - (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
            else
                margin = 0;

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.gameActivityBlackCardInfo.getLayoutParams();
            params.bottomMargin = margin;
            binding.gameActivityBlackCardInfo.setLayoutParams(params);
        });
    }
    //endregion

    //region Lobby
    public void showLobby() {
        setBlackCard(null);
        clearTable();
        clearHand();
        showTable(false);
        resetTimer();

        binding.gameActivityNotLobby.setVisibility(View.GONE);
        binding.gameActivityLobby.setVisibility(View.VISIBLE);

        binding.gameActivityPlayers.setAdapter(null);
        binding.gameActivityLobbyPlayers.setAdapter(playersAdapter);

        if (customDecksEnabled && listener != null && listener.amHost())
            binding.gameActivityCustomDecks.setVisibility(View.VISIBLE);
        else
            binding.gameActivityCustomDecks.setVisibility(View.GONE);
    }

    public void hideLobby() {
        binding.gameActivityNotLobby.setVisibility(View.VISIBLE);
        binding.gameActivityLobby.setVisibility(View.GONE);

        binding.gameActivityLobbyPlayers.setAdapter(null);
        binding.gameActivityPlayers.setAdapter(playersAdapter);
    }
    //endregion

    public interface Listener {
        void onCardSelected(@NonNull BaseCard card);

        void showOptions(boolean goToCustomDecks);

        void startGame();

        boolean isLobby();

        boolean amHost();

        void onPlayerSelected(@NonNull String name);

        /**
         * @return Whether the operation was successful
         */
        boolean onStarCard(@NotNull CardsGroup group);
    }

    @UiThread
    private class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> {
        private final List<CardsGroup> cards = new ArrayList<>(10);
        private RecyclerView list;
        private boolean selectable = false;

        CardsAdapter() {
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CardsGroup group = cards.get(position);
            holder.group.setCards(group);
            holder.group.setLeftAction(R.drawable.baseline_star_outline_24, card -> {
                if (listener != null && listener.onStarCard(group))
                    holder.group.setLeftAction(R.drawable.baseline_star_24, null);
            });
            holder.group.setOnClickListener((NewCardsGroupView.OnClickListener) card -> {
                if (listener != null && selectable) listener.onCardSelected(card);
            });
            holder.group.setSelectable(selectable);
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView list) {
            super.onAttachedToRecyclerView(list);
            this.list = list;
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView list) {
            super.onDetachedFromRecyclerView(list);
            this.list = null;
        }

        void clear() {
            cards.clear();
            notifyDataSetChanged();
        }

        void addGroup(@NotNull CardsGroup group) {
            cards.add(0, group);
            notifyItemInserted(0);
        }

        void addSingles(@NonNull List<BaseCard> list) {
            for (BaseCard card : list) cards.add(0, CardsGroup.singleton(card));
            notifyItemRangeInserted(0, list.size());
        }

        void removeWithGroup(@NotNull BaseCard card) {
            for (int i = 0; i < cards.size(); i++) {
                CardsGroup group = cards.get(i);
                if (group.contains(card)) {
                    cards.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }

        void setGroups(@NotNull List<CardsGroup> list) {
            cards.clear();
            cards.addAll(list);
            notifyDataSetChanged();
        }

        void setSingles(@NonNull List<BaseCard> list) {
            cards.clear();
            for (BaseCard card : list) cards.add(CardsGroup.singleton(card));
            notifyDataSetChanged();
        }

        @NotNull
        List<BaseCard> findAndRemoveFaceUpCards() {
            List<BaseCard> faceUp = new ArrayList<>();

            for (int i = 0; i < cards.size(); i++) {
                CardsGroup group = cards.get(i);
                if (!group.isUnknwon()) {
                    faceUp.addAll(group);
                    cards.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }

            return faceUp;
        }

        void setSelectable(boolean selectable) {
            if (this.selectable == selectable)
                return;

            this.selectable = selectable;
            notifyItemRangeChanged(0, cards.size());
        }

        public void notifyWinnerCard(int winnerCardId) {
            for (int i = 0; i < cards.size(); i++) {
                CardsGroup group = cards.get(i);
                if (group.hasCard(winnerCardId)) {
                    if (list != null && list.getLayoutManager() instanceof LinearLayoutManager) { // Scroll only if item is not visible
                        LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
                        int start = llm.findFirstCompletelyVisibleItemPosition();
                        int end = llm.findLastCompletelyVisibleItemPosition();
                        if (start == -1 || end == -1 || i >= end || i <= start)
                            list.getLayoutManager().smoothScrollToPosition(list, null, i);
                    }

                    for (BaseCard card : group)
                        if (card instanceof GameCard)
                            ((GameCard) card).setWinner();

                    notifyItemChanged(i);
                    break;
                }
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final NewCardsGroupView group;

            ViewHolder(@NonNull ViewGroup parent) {
                super(new NewCardsGroupView(parent.getContext()));
                group = (NewCardsGroupView) itemView;
            }
        }
    }

    private class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final List<GameInfo.Player> list;

        PlayersAdapter(@NotNull List<GameInfo.Player> list) {
            this.list = list;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GameInfo.Player player = list.get(position);
            holder.binding.playerItemName.setText(player.name);
            GlideUtils.loadProfileImage(holder.binding.playerItemImage, player);

            if (OverloadedApi.get().isOverloadedUserOnServerCached(player.name))
                CommonUtils.setTextColor(holder.binding.playerItemName, R.color.appColor_500);
            else
                holder.binding.playerItemName.setTextColor(Color.BLACK);

            holder.binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onPlayerSelected(player.name);
            });

            holder.update(player, listener != null && listener.isLobby());
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                GameInfo.Player player = (GameInfo.Player) payloads.get(0);
                holder.update(player, listener != null && listener.isLobby());
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemNewPlayerBinding binding;

            ViewHolder(@NonNull ViewGroup parent) {
                super(inflater.inflate(R.layout.item_new_player, parent, false));
                binding = ItemNewPlayerBinding.bind(itemView);
            }

            void update(@NotNull GameInfo.Player player, boolean lobby) {
                binding.playerItemPoints.setText(String.valueOf(player.score));

                int iconRes;
                if (lobby) {
                    binding.playerItemPoints.setVisibility(View.INVISIBLE);

                    if (player.status == GameInfo.PlayerStatus.HOST)
                        iconRes = R.drawable.ic_status_idle_24;
                    else
                        iconRes = 0;
                } else {
                    binding.playerItemPoints.setVisibility(View.VISIBLE);

                    switch (player.status) {
                        case WINNER:
                            iconRes = R.drawable.ic_status_winner_24;
                            break;
                        case HOST:
                            iconRes = R.drawable.ic_status_idle_24;
                            break;
                        case IDLE:
                            iconRes = R.drawable.ic_status_done_24;
                            break;
                        case JUDGE:
                        case JUDGING:
                            iconRes = R.drawable.ic_status_hammer_24;
                            break;
                        default:
                        case SPECTATOR:
                        case PLAYING:
                            iconRes = 0;
                            break;
                    }
                }

                if (iconRes == 0) {
                    binding.playerItemStatus.setVisibility(View.GONE);
                } else {
                    binding.playerItemStatus.setVisibility(View.VISIBLE);
                    binding.playerItemStatus.setImageResource(iconRes);
                }
            }
        }
    }

    private class CountdownTask extends TimerTask {
        private final AtomicInteger count;

        CountdownTask(int sec) {
            this.count = new AtomicInteger(sec);
        }

        @Override
        public void run() {
            int val = count.get();
            if (val >= 0) {
                handler.post(() -> binding.gameActivityCounter.setText(String.format(Locale.getDefault(), "%d\u00A0", val)));
            } else if (val == -3) {
                cancel();
            }

            count.decrementAndGet();
        }
    }
}
