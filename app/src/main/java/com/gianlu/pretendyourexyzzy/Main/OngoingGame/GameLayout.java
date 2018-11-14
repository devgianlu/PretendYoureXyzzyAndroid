package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

@UiThread
public class GameLayout extends FrameLayout implements CardsAdapter.Listener {
    private final FloatingActionButton startGame;
    private final GameCardView blackCard;
    private final TextView instructions;
    private final RecyclerView whiteCardsList;
    private final RecyclerView playersList;
    private final TextView time;
    private PlayersAdapter playersAdapter;
    private PlayersAdapter.Listener playersListener;
    private CardsAdapter tableAdapter;
    private CardsAdapter handAdapter;
    private Listener listener;

    public GameLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public GameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View.inflate(context, R.layout.game_layout, this);

        blackCard = findViewById(R.id.gameLayout_blackCard);
        instructions = findViewById(R.id.gameLayout_instructions);
        time = findViewById(R.id.gameLayout_time);
        startGame = findViewById(R.id.gameLayout_startGame);

        whiteCardsList = findViewById(R.id.gameLayout_whiteCards);
        whiteCardsList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        playersList = findViewById(R.id.gameLayout_players);
        playersList.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        //  Utils.removeAllDecorations(playersList);
        playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
    }

    public void attach(@NonNull PlayersAdapter.Listener listener) {
        playersListener = listener;
    }

    public void attach(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setup(@NonNull SensitiveGameData gameData) {
        playersAdapter = new PlayersAdapter(getContext(), gameData.players, playersListener);
        gameData.playersInterface = playersAdapter;
        playersList.setAdapter(playersAdapter);

        tableAdapter = new CardsAdapter(getContext(), GameCardView.Action.SELECT, GameCardView.Action.TOGGLE_STAR, this);

        handAdapter = new CardsAdapter(getContext(), GameCardView.Action.SELECT, GameCardView.Action.TOGGLE_STAR, this);
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

    public void setTable(List<CardsGroup> cards) {
        tableAdapter.setCardGroups(cards, null);
    }

    public void addHand(List<Card> cards) {
        handAdapter.addCards(cards);
    }

    public void setBlackCard(@Nullable Card card) {
        blackCard.setCard(card);
    }

    public void removeHand(@NonNull BaseCard card) {
        handAdapter.removeCard(card);
    }

    public void addTable(BaseCard card) {
        tableAdapter.addCards(Collections.singletonList((Card) card));
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

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return whiteCardsList;
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (action == GameCardView.Action.SELECT)
            listener.onCardSelected(card);
    }

    public interface Listener {
        void onCardSelected(@NonNull BaseCard card);
    }
}
