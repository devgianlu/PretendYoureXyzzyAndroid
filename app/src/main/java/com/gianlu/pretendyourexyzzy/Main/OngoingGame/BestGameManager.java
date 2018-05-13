package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.Cards.CardsGroup;
import com.gianlu.pretendyourexyzzy.Cards.GameCardView;
import com.gianlu.pretendyourexyzzy.Cards.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Locale;
import java.util.logging.Logger;

public class BestGameManager implements Pyx.OnEventListener {
    private final static String POLLING = BestGameManager.class.getName();
    private final static Logger logger = Logger.getLogger(BestGameManager.class.getName());
    private final Ui ui;
    private final Data data;
    private final Listener listener;
    private final RegisteredPyx pyx;
    private final Context context;

    public BestGameManager(Context context, ViewGroup layout, RegisteredPyx pyx, GameInfoAndCards bundle, Listener listener) {
        this.context = context;
        this.ui = new Ui(layout);
        this.pyx = pyx;
        this.data = new Data(bundle);
        this.listener = listener;

        this.pyx.polling().addListener(POLLING, this);
    }

    @Override
    public void onPollMessage(PollMessage message) {
        if (message.event != PollMessage.Event.CHAT)
            logger.fine(message.event.name() + " -> " + message.obj);

        switch (message.event) {
            case GAME_BLACK_RESHUFFLE:
            case GAME_SPECTATOR_JOIN:
            case CARDCAST_REMOVE_CARDSET:
            case CARDCAST_ADD_CARDSET:
            case GAME_WHITE_RESHUFFLE:
            case GAME_STATE_CHANGE:
            case HAND_DEAL:
            case GAME_OPTIONS_CHANGED:
            case GAME_SPECTATOR_LEAVE:
            case GAME_JUDGE_LEFT:
            case GAME_PLAYER_SKIPPED:
            case GAME_JUDGE_SKIPPED:
            case GAME_ROUND_COMPLETE:
            case GAME_PLAYER_INFO_CHANGE:
            case GAME_PLAYER_JOIN:
            case GAME_PLAYER_LEAVE:
            case GAME_PLAYER_KICKED_IDLE:
            case HURRY_UP:
            case KICKED_FROM_GAME_IDLE:
                break;
        }
    }

    @Override
    public void onStoppedPolling() {
        // TODO
    }

    private void startGame() {
        // TODO
    }

    @NonNull
    public GameInfo gameInfo() {
        return data.info;
    }

    @NonNull
    public View getStartGameButton() {
        return ui.startGame;
    }

    private enum UiEvent {
        JUDGE("You're the Card Czar! Waiting for other players...", Kind.TEXT),
        SELECT_WINNING_CARD("Select the winning card(s).", Kind.TEXT),
        YOU_ROUND_WINNER("You won this round! A new round will begin shortly...", "You won this round!"),
        SPECTATOR("You're a spectator.", Kind.TEXT),
        GAME_HOST("You're the game host! Start the game when you're ready.", Kind.TEXT),
        WAITING_FOR_ROUND_TO_END("Waiting for the current round to end...", Kind.TEXT),
        WAITING_FOR_START("Waiting for the game to start...", Kind.TEXT),
        JUDGE_LEFT("Judge %s left. A new round will begin shortly.", "Judge %s left."),
        IS_JUDGING("%s is judging...", Kind.TEXT),
        ROUND_WINNER("%s won this round! A new round will begin shortly...", "%s won this round!"),
        WAITING_FOR_OTHER_PLAYERS("Waiting for other players...", Kind.TEXT),
        PLAYER_SKIPPED("%s has been skipped.", Kind.TOAST),
        PICK_CARDS("Select %d card(s) to play. Your hand:", Kind.TEXT),
        JUDGE_SKIPPED("Judge %s has been skipped.", Kind.TOAST),
        GAME_WINNER("%s won the game! Waiting for the host to start a new game...", "%s won the game!"),
        YOU_GAME_WINNER("You won the game! Waiting for the host to start a new game...", "You won the game!");

        private final String toast;
        private final String text;
        private final Kind kind;

        UiEvent(String text, Kind kind) {
            this.text = text;
            this.kind = kind;
            this.toast = null;
        }

        UiEvent(String text, String toast) {
            this.toast = toast;
            this.text = text;
            this.kind = Kind.BOTH;
        }

        public enum Kind {
            TOAST,
            TEXT,
            BOTH
        }
    }

    public interface Listener {
        void shouldLeaveGame(); // TODO
    }

    private class Data implements CardsAdapter.Listener {
        private final GameCards cards;
        private final GameInfo info;
        private final CardsAdapter handAdapter;
        private final CardsAdapter tableAdapter;
        private final PlayersAdapter playersAdapter;

        Data(GameInfoAndCards bundle) {
            cards = bundle.cards;
            info = bundle.info;

            handAdapter = new CardsAdapter(context, true, PyxCardsGroupView.Action.TOGGLE_STAR, this);
            tableAdapter = new CardsAdapter(context, true, PyxCardsGroupView.Action.TOGGLE_STAR, this);
            playersAdapter = new PlayersAdapter(context, info.players);
        }

        @Nullable
        @Override
        public RecyclerView getCardsRecyclerView() {
            return ui.whiteCardsList;
        }

        @Override
        public void onCardAction(PyxCardsGroupView.Action action, CardsGroup<? extends BaseCard> group, BaseCard card) {
            // Called by table and by hand
        }
    }

    private class Ui {
        private final FloatingActionButton startGame;
        private final GameCardView blackCard;
        private final TextView instructions;
        private final RecyclerView whiteCardsList;
        private final RecyclerView playersList;

        Ui(ViewGroup layout) {
            blackCard = layout.findViewById(R.id.gameLayout_blackCard);
            instructions = layout.findViewById(R.id.gameLayout_instructions);

            startGame = layout.findViewById(R.id.gameLayout_startGame);
            startGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startGame();
                }
            });

            whiteCardsList = layout.findViewById(R.id.gameLayout_whiteCards);
            whiteCardsList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            playersList = layout.findViewById(R.id.gameLayout_players);
            playersList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        }

        //****************//
        // Public methods //
        //****************//

        public void blackCard(@Nullable Card card) {
            blackCard.setCard(card);
        }

        public void event(@NonNull UiEvent ev, Object... args) {
            switch (ev.kind) {
                case BOTH:
                    uiText(ev.text, args);
                    uiToast(ev.toast, args);
                    break;
                case TOAST:
                    uiToast(ev.text, args);
                    break;
                case TEXT:
                    uiText(ev.text, args);
                    break;
            }
        }

        //*****************//
        // Private methods //
        //*****************//

        private void uiToast(@NonNull String text, Object... args) {
            Toaster.show(context, String.format(Locale.getDefault(), text, args), Toast.LENGTH_SHORT, null, null, null);
        }

        private void uiText(@NonNull String text, Object... args) {
            instructions.setText(String.format(Locale.getDefault(), text, args));
        }
    }
}
