package com.gianlu.pretendyourexyzzy.game;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.UnknownCard;
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

public class GameUi {
    private final Context context;
    private final com.gianlu.pretendyourexyzzy.databinding.ActivityNewGameBinding binding;
    private final Timer timer = new Timer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Listener listener = null;
    private CountdownTask countdownTask;
    private PlayersAdapter playersAdapter;
    private CardsAdapter tableAdapter;
    private CardsAdapter handAdapter;
    private BaseCard blackCard;

    public GameUi(@NotNull Context context, @NotNull ActivityNewGameBinding binding, @NonNull RegisteredPyx pyx) {
        this.context = context;
        this.binding = binding;

        binding.gameActivityPlayers.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        binding.gameActivityLobbyPlayers.setLayoutManager(new GridLayoutManager(context, 3, RecyclerView.VERTICAL, false));

        binding.gameActivityStart.setOnClickListener(v -> {
            if (listener != null) listener.startGame();
        });
        binding.gameActivityOptions.setOnClickListener(v -> {
            if (listener != null) listener.showOptions();
        });

        if (pyx.config().crCastEnabled() || pyx.config().customDecksEnabled()) {
            SpannableString clickableSpan = SpannableString.valueOf(context.getString(R.string.customDecks));
            clickableSpan.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.appColor_400)), 0, clickableSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            clickableSpan.setSpan(new ClickSpan(() -> {
                if (listener != null) listener.showCustomDecks();
            }), 0, clickableSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            binding.gameActivityCustomDecks.setMovementMethod(LinkMovementMethod.getInstance());
            binding.gameActivityCustomDecks.setVisibility(View.VISIBLE);
            binding.gameActivityCustomDecks.setText(new SpannableStringBuilder()
                    .append(context.getString(R.string.playWith))
                    .append(clickableSpan));
        } else {
            binding.gameActivityCustomDecks.setVisibility(View.GONE);
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

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

    //region Black card
    public void setBlackCard(@Nullable BaseCard card) {
        this.blackCard = card;

        if (card != null) {
            binding.gameActivityBlackCardText.setText(card.textUnescaped());
            binding.gameActivityBlackCardPick.setHtml(R.string.numPick, card.numPick());
            binding.gameActivityBlackCardWatermark.setText(card.watermark());
        } else {
            binding.gameActivityBlackCardText.setText(null);
            binding.gameActivityBlackCardPick.setHtml(null);
            binding.gameActivityBlackCardWatermark.setText(null);
        }
    }
    //endregion

    @Nullable
    public BaseCard blackCard() {
        return blackCard;
    }

    //region White cards
    public void clearHand() {
        handAdapter.clear();
    }
    //endregion

    public void showHand(boolean selectable) {
        handAdapter.setSelectable(selectable);
        binding.gameActivityWhiteCards.swapAdapter(handAdapter, true);
    }

    public void setHand(@NotNull List<BaseCard> cards) {
        handAdapter.setSingles(cards);
    }

    public void addHand(@NotNull List<BaseCard> cards) {
        handAdapter.addSingles(cards);
    }

    public void removeHand(@NotNull BaseCard card) {
        handAdapter.removeWithGroup(card);
    }

    public void clearTable() {
        tableAdapter.clear();
    }

    public void showTable(boolean selectable) {
        tableAdapter.setSelectable(selectable);
        binding.gameActivityWhiteCards.swapAdapter(tableAdapter, true);
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
    }

    public void addTable(@NotNull GameCard card, BaseCard blackCard) {
        if (blackCard != null && blackCard.numPick() > 1) {
            List<BaseCard> cards = tableAdapter.findAndRemoveFaceUpCards();
            cards.add(card);
            tableAdapter.addSingles(cards);
        } else {
            tableAdapter.addSingles(Collections.singletonList(card));
        }
    }

    public void addBlankCardTable() {
        if (blackCard != null) tableAdapter.addGroup(CardsGroup.unknown(blackCard.numPick()));
    }

    public void notifyWinnerCard(int winnerCardId) {
        if (tableAdapter != null) tableAdapter.notifyWinnerCard(winnerCardId);
    }

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
    }
    //endregion

    public void hideLobby() {
        binding.gameActivityNotLobby.setVisibility(View.VISIBLE);
        binding.gameActivityLobby.setVisibility(View.GONE);

        binding.gameActivityLobbyPlayers.setAdapter(null);
        binding.gameActivityPlayers.setAdapter(playersAdapter);
    }

    public interface Listener {
        void onCardSelected(@NonNull BaseCard card);

        void showOptions();

        void startGame();

        boolean isLobby();

        void onPlayerSelected(@NonNull String name);

        void showCustomDecks();
    }
    //endregion

    private static class ClickSpan extends ClickableSpan {
        private final OnClickListener mListener;

        ClickSpan(@NonNull OnClickListener listener) {
            mListener = listener;
        }

        @Override
        public void onClick(@NonNull View widget) {
            mListener.onClick();
        }

        public interface OnClickListener {
            void onClick();
        }
    }

    @UiThread
    private class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> {
        private final List<CardsGroup> cards = new ArrayList<>(10);
        private RecyclerView list;

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

            // TODO: Setup card group and call listener
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
            // TODO: Set cards selectable
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
            ViewHolder(@NonNull ViewGroup parent) {
                super(new View(parent.getContext()) /* TODO: Game card group item */);
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
                super(inflater.inflate(R.layout.item_grid_player, parent, false));
                binding = ItemNewPlayerBinding.bind(((ViewGroup) itemView).getChildAt(0));
            }

            void update(@NotNull GameInfo.Player player, boolean lobby) {
                binding.playerItemPoints.setText(String.valueOf(player.score));

                int iconRes;
                if (lobby) {
                    binding.playerItemPoints.setVisibility(View.INVISIBLE);
                    iconRes = 0;
                } else {
                    binding.playerItemPoints.setVisibility(View.VISIBLE);

                    switch (player.status) {
                        case WINNER:
                            iconRes = R.drawable.baseline_star_24; // TODO
                            break;
                        case HOST:
                            iconRes = R.drawable.baseline_person_24; // TODO
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
