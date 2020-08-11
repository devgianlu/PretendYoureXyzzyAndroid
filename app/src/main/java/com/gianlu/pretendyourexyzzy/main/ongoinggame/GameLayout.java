package com.gianlu.pretendyourexyzzy.main.ongoinggame;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.dialogs.CardImageZoomDialog;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@UiThread
public class GameLayout extends FrameLayout implements CardsAdapter.Listener {
    private final FloatingActionButton startGame;
    private final GameCardView blackCard;
    private final TextView instructions;
    private final RecyclerView whiteCardsList;
    private final RecyclerView playersList;
    private final TextView time;
    private final Timer timer = new Timer();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StarredCardsDatabase starredCards;
    private PlayersAdapter.Listener playersListener;
    private CardsAdapter tableAdapter;
    private CardsAdapter handAdapter;
    private Listener listener;
    private CountdownTask currentTask;

    public GameLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public GameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View.inflate(context, R.layout.game_layout, this);

        starredCards = StarredCardsDatabase.get(context);

        blackCard = findViewById(R.id.gameLayout_blackCard);
        instructions = findViewById(R.id.gameLayout_instructions);
        time = findViewById(R.id.gameLayout_time);
        startGame = findViewById(R.id.gameLayout_startGame);

        whiteCardsList = findViewById(R.id.gameLayout_whiteCards);
        whiteCardsList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        playersList = findViewById(R.id.gameLayout_players);
        playersList.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        startGame.setOnClickListener(v -> {
            if (listener != null) listener.startGame();
        });
    }

    @NonNull
    public RecyclerView getCardsRecyclerView() {
        return whiteCardsList;
    }

    public void countFrom(int ms) {
        if (currentTask != null) currentTask.cancel();
        if (ms < 2147000) {
            currentTask = new CountdownTask(ms / 1000);
            timer.scheduleAtFixedRate(currentTask, 0, 1000);
        } else {
            time.setText("∞");
        }
    }

    public void attach(@NonNull PlayersAdapter.Listener listener) {
        playersListener = listener;
    }

    public void attach(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setup(@NonNull SensitiveGameData gameData) {
        PlayersAdapter playersAdapter = new PlayersAdapter(getContext(), gameData.players, playersListener);
        gameData.playersInterface = playersAdapter;
        playersList.setAdapter(playersAdapter);

        tableAdapter = new CardsAdapter(GameCardView.Action.SELECT, GameCardView.Action.TOGGLE_STAR, this);
        handAdapter = new CardsAdapter(GameCardView.Action.SELECT, GameCardView.Action.TOGGLE_STAR, this);
    }

    @NonNull
    public View getStartGameButton() {
        return startGame;
    }

    void startGameVisible(boolean visible) {
        if (visible) startGame.show();
        else startGame.hide();
    }

    public void showTable(boolean selectable) {
        tableAdapter.setSelectable(selectable);
        whiteCardsList.swapAdapter(tableAdapter, true);
    }

    public void showHand(boolean selectable) {
        handAdapter.setSelectable(selectable);
        whiteCardsList.swapAdapter(handAdapter, true);
    }

    public void setTable(List<CardsGroup> cards, @Nullable BaseCard blackCard) {
        tableAdapter.setCardGroups(cards, blackCard);
    }

    public void addHand(List<? extends BaseCard> cards) {
        handAdapter.addCardsAsSingleton(cards);
    }

    public void setHand(List<? extends BaseCard> cards) {
        clearHand();
        addHand(cards);
    }

    public void setBlackCard(@Nullable BaseCard card) {
        blackCard.setCard(card);
    }

    public void removeHand(@NonNull BaseCard card) {
        handAdapter.removeCard(card);
    }

    public void clearHand() {
        handAdapter.clear();
    }

    public void addTable(@NonNull BaseCard card, @Nullable BaseCard blackCard) {
        if (blackCard != null && blackCard.numPick() > 1) {
            List<BaseCard> cards = tableAdapter.findAndRemoveFaceUpCards();
            cards.add(card);
            tableAdapter.addCardsAsGroup(cards);
        } else {
            tableAdapter.addCard(card);
        }
    }

    public void notifyWinnerCard(int winnerCard) {
        tableAdapter.notifyWinningCard(winnerCard);
    }

    public void clearTable() {
        tableAdapter.clear();
    }

    public void addBlankCardTable() {
        BaseCard card = blackCard.getCard();
        if (card != null) tableAdapter.addBlankCards(card);
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (action == GameCardView.Action.SELECT) {
            if (listener != null) listener.onCardSelected(card);
        } else if (action == GameCardView.Action.TOGGLE_STAR) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_STARRED_CARD_ADD);

            BaseCard bc = blackCard();
            if (bc != null && starredCards.putCard((GameCard) bc, group))
                Toaster.with(getContext()).message(R.string.addedCardToStarred).show();
        } else if (action == GameCardView.Action.SELECT_IMG) {
            listener.showDialog(CardImageZoomDialog.get(card));
        }
    }

    public void setInstructions(@StringRes int text, Object... args) {
        instructions.setText(getContext().getString(text, args));
    }

    @Nullable
    public BaseCard blackCard() {
        return blackCard.getCard();
    }

    public void resetTimer() {
        if (currentTask != null) currentTask.cancel();
        time.setText("");
    }

    @NonNull
    public RecyclerView getPlayersRecyclerView() {
        return playersList;
    }

    @NonNull
    public GameCardView getBlackCardView() {
        return blackCard;
    }

    public interface Listener {
        void onCardSelected(@NonNull BaseCard card);

        void showDialog(@NonNull DialogFragment dialog);

        void startGame();

        void refreshGameInfo();
    }

    private class CountdownTask extends TimerTask {
        private int count;

        CountdownTask(int sec) {
            this.count = sec;
        }

        @Override
        public void run() {
            if (count >= 0) {
                String countStr = String.valueOf(count);
                handler.post(() -> time.setText(countStr));
            } else if (count == -3) {
                cancel();
                listener.refreshGameInfo();
            }

            count--;
        }
    }
}
