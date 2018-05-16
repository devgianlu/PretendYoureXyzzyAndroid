package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.Cards.CardsGroup;
import com.gianlu.pretendyourexyzzy.Cards.GameCardView;
import com.gianlu.pretendyourexyzzy.Cards.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BestGameManager implements Pyx.OnEventListener {
    private final static String POLLING = BestGameManager.class.getName();
    private final Ui ui;
    private final Data data;
    private final Listener listener;
    private final RegisteredPyx pyx;
    private final Context context;

    public BestGameManager(Context context, ViewGroup layout, RegisteredPyx pyx, GameInfoAndCards bundle, Listener listener) {
        this.context = context;
        this.pyx = pyx;
        this.listener = listener;
        this.ui = new Ui(layout);
        this.data = new Data(bundle);

        this.pyx.polling().addListener(POLLING, this);
        this.data.setup();
    }

    @Override
    public void onPollMessage(PollMessage msg) throws JSONException {
        if (msg.event != PollMessage.Event.CHAT)
            System.out.println(msg.event.name() + " -> " + msg.obj);

        switch (msg.event) {
            case HAND_DEAL:
                data.handDeal(CommonUtils.toTList(msg.obj.getJSONArray("h"), Card.class));
                break;
            case GAME_STATE_CHANGE:
                data.gameStateChanged(Game.Status.parse(msg.obj.getString("gs")), msg.obj);
                break;
            case GAME_PLAYER_INFO_CHANGE:
                data.gamePlayerInfoChanged(new GameInfo.Player(msg.obj.getJSONObject("pi")));
                break;
            case GAME_ROUND_COMPLETE:
                data.gameRoundComplete(msg.obj.getString("rw"), msg.obj.getInt("WC"), msg.obj.getString("rP"), msg.obj.getInt("i"));
                break;
            case GAME_OPTIONS_CHANGED:
                data.gameOptionsChanged(new Game.Options(msg.obj.getJSONObject("go")));
                break;
            case HURRY_UP:
                ui.event(UiEvent.HURRY_UP);
                break;
            case GAME_PLAYER_JOIN:
                data.gamePlayerJoin(new GameInfo.Player(msg.obj.getString("n"), 0, GameInfo.PlayerStatus.IDLE));
                break;
            case GAME_PLAYER_LEAVE:
                data.gamePlayerLeave(msg.obj.getString("n"));
                break;
            case GAME_PLAYER_SKIPPED:
                ui.event(UiEvent.PLAYER_SKIPPED, msg.obj.getString("n"));
                break;
            case GAME_JUDGE_LEFT:
                data.gameJudgeLeft();
                break;
            case GAME_JUDGE_SKIPPED:
                data.gameJudgeSkipped();
                break;
            case GAME_PLAYER_KICKED_IDLE:
                ui.event(UiEvent.PLAYER_KICKED, msg.obj.getString("n"));
                break;
            case GAME_BLACK_RESHUFFLE:
            case GAME_WHITE_RESHUFFLE:
                break;
            case GAME_SPECTATOR_JOIN:
            case GAME_SPECTATOR_LEAVE:
                break;
            case CARDCAST_REMOVE_CARDSET:
            case CARDCAST_ADD_CARDSET:
                break;
            case CHAT:
            case BANNED:
            case GAME_LIST_REFRESH:
            case KICKED:
            case NEW_PLAYER:
            case NOOP:
            case PLAYER_LEAVE:
            case KICKED_FROM_GAME_IDLE:
                break;
        }
    }

    @Override
    public void onStoppedPolling() {
        // TODO
    }

    private void startGame() {
        pyx.request(PyxRequests.startGame(gid()), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                Toaster.show(context, Utils.Messages.GAME_STARTED);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (!(ex instanceof PyxException) || !ui.handleStartGameException((PyxException) ex))
                    Toaster.show(context, Utils.Messages.FAILED_START_GAME, ex);
            }
        });
    }

    @NonNull
    public GameInfo gameInfo() {
        return data.info;
    }

    @NonNull
    public View getStartGameButton() {
        return ui.startGame;
    }

    @NonNull
    public String me() {
        return pyx.user().nickname;
    }

    @NonNull
    public String host() {
        return gameInfo().game.host;
    }

    public int gid() {
        return gameInfo().game.gid;
    }

    private enum UiEvent { // TODO: If we could use res this would be perfect
        YOU_JUDGE("You're the Card Czar! Waiting for other players...", Kind.TEXT),
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
        YOU_GAME_WINNER("You won the game! Waiting for the host to start a new game...", "You won the game!"),
        NOT_YOUR_TURN("Not your turn!", Kind.TOAST),
        HURRY_UP("Hurry up!", Kind.TOAST),
        PLAYER_KICKED("%s has been kicked for being idle.", Kind.TOAST);

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

        void showDialog(AlertDialog.Builder dialog);
    }

    private class Data implements CardsAdapter.Listener {
        private final GameInfo info;
        private final CardsAdapter handAdapter;
        private final CardsAdapter tableAdapter;
        private final PlayersAdapter playersAdapter;
        private int judgeIndex = 0;

        Data(GameInfoAndCards bundle) {
            GameCards cards = bundle.cards;
            info = bundle.info;

            playersAdapter = new PlayersAdapter(context, info.players); // TODO: We could have one list instance
            playersAdapter.setPlayers(info.players);
            ui.playersList.setAdapter(playersAdapter);

            handAdapter = new CardsAdapter(context, true, PyxCardsGroupView.Action.TOGGLE_STAR, this);
            handAdapter.setCards(cards.hand);

            tableAdapter = new CardsAdapter(context, true, PyxCardsGroupView.Action.TOGGLE_STAR, this);
            tableAdapter.setCardGroups(cards.whiteCards);

            ui.blackCard(cards.blackCard);
        }

        public void setup() { // Called ONLY from constructor
            GameInfo.Player me = info.player(me());
            if (me != null) {
                switch (me.status) {
                    case JUDGE:
                    case JUDGING:
                        ui.showTableCards();
                        break;
                    case PLAYING:
                        ui.showHandCards();
                    case IDLE:
                        ui.showTableCards();
                        break;
                    case HOST:
                    case WINNER:
                    case SPECTATOR:
                        break;
                }
            }

            for (int i = 0; i < info.players.size(); i++) {
                GameInfo.Player player = info.players.get(i);
                switch (player.status) {
                    case JUDGE:
                    case JUDGING:
                        judgeIndex = i;
                        break;
                    case HOST:
                    case IDLE:
                    case PLAYING:
                    case WINNER:
                    case SPECTATOR:
                        break;
                }
            }

            ui.setStartGameVisible(info.game.status == Game.Status.LOBBY && Objects.equals(host(), me()));
        }

        public void gameStateChanged(@NonNull Game.Status status, @NonNull JSONObject obj) throws JSONException {
            info.game.status = status;

            ui.setStartGameVisible(status == Game.Status.LOBBY && Objects.equals(host(), me()));
            switch (status) {
                case PLAYING:
                    playingState(new Card(obj.getJSONObject("bc")));
                    nextRound();
                    break;
                case JUDGING:
                    judgingState(CardsGroup.list(obj.getJSONArray("wc")));
                    break;
                case LOBBY:
                    break;
                case DEALING:
                case ROUND_OVER:
                    break;
            }
        }

        public void nextRound() {
            judgeIndex++;
            if (judgeIndex >= info.players.size()) judgeIndex = 0;

            for (int i = 0; i < info.players.size(); i++) {
                GameInfo.Player player = info.players.get(i);
                if (i == judgeIndex) player.status = GameInfo.PlayerStatus.JUDGE;
                else player.status = GameInfo.PlayerStatus.PLAYING;
                gamePlayerInfoChanged(player); // Allowed, not notified by server
            }

            tableAdapter.clear();
        }

        public void handDeal(List<Card> cards) { // FIXME: Can be done more reliably (with game status?)
            if (cards.size() == 10) handAdapter.setCards(cards);
            else handAdapter.addCards(cards);
        }

        private void playingState(@NonNull Card blackCard) {
            ui.blackCard(blackCard);
        }

        private void judgingState(List<CardsGroup> cards) {
            tableAdapter.setCardGroups(cards);
        }

        public void gameRoundComplete(String roundWinner, int winningCard, String roundPermalink, int intermission) {
            if (Objects.equals(roundWinner, me())) ui.event(UiEvent.YOU_ROUND_WINNER);
            else ui.event(UiEvent.ROUND_WINNER, roundWinner);

            tableAdapter.notifyWinningCard(winningCard);
        }

        public void gamePlayerInfoChanged(@NonNull GameInfo.Player player) {
            playersAdapter.playerChanged(player);
            info.playerChanged(player);

            switch (player.status) {
                case JUDGING:
                    if (Objects.equals(player.name, me())) {
                        ui.showTableCards();
                        ui.event(UiEvent.SELECT_WINNING_CARD);
                    } else {
                        ui.event(UiEvent.IS_JUDGING, player.name);
                    }
                    break;
                case JUDGE:
                    if (Objects.equals(player.name, me())) {
                        ui.showTableCards();
                        ui.event(UiEvent.YOU_JUDGE);
                    }

                    judgeIndex = Utils.indexOf(info.players, player.name); // Redundant, but for safety...
                    break;
                case IDLE:
                    if (Objects.equals(player.name, me())) {
                        ui.showTableCards();
                        ui.event(UiEvent.WAITING_FOR_OTHER_PLAYERS);
                    }

                    if (info.game.status == Game.Status.PLAYING)
                        tableAdapter.addBlankCard();
                    break;
                case PLAYING:
                    if (Objects.equals(player.name, me())) {
                        ui.showHandCards();
                        BaseCard bc = ui.blackCard();
                        if (bc != null) ui.event(UiEvent.PICK_CARDS, bc.numPick());
                    }
                    break;
                case HOST:
                case WINNER:
                case SPECTATOR:
                    break;
            }
        }

        public void gamePlayerJoin(@NonNull GameInfo.Player player) {
            info.newPlayer(player);
            playersAdapter.newPlayer(player);
        }

        public void gamePlayerLeave(@NonNull String nick) {
            info.removePlayer(nick);
            playersAdapter.removePlayer(nick);

            int pos = Utils.indexOf(info.players, nick);
            if (pos < judgeIndex) judgeIndex--;
        }

        @Nullable
        @Override
        public RecyclerView getCardsRecyclerView() {
            return ui.whiteCardsList;
        }

        @Override
        public void onCardAction(PyxCardsGroupView.Action action, CardsGroup group, BaseCard card) {
            if (action == PyxCardsGroupView.Action.SELECT) {
                GameInfo.Player me = info.player(me());
                if (me != null) {
                    if (me.status == GameInfo.PlayerStatus.PLAYING && info.game.status == Game.Status.PLAYING)
                        ui.playCard(card);
                    else if (me.status == GameInfo.PlayerStatus.JUDGING && info.game.status == Game.Status.JUDGING)
                        ui.judgeSelectCard(card);
                    else
                        ui.event(UiEvent.NOT_YOUR_TURN);
                }
            } else if (action == PyxCardsGroupView.Action.TOGGLE_STAR) {
                // TODO
            }
        }

        public void gameOptionsChanged(@NonNull Game.Options options) {
            info.game.options = options;
        }

        public void gameJudgeLeft() {
            GameInfo.Player judge = info.players.get(judgeIndex);
            ui.event(UiEvent.JUDGE_LEFT, judge.name);
            judgeIndex--; // Will be incremented by #nextRound()

            tableAdapter.clear();
            ui.showTableCards();
        }

        public void gameJudgeSkipped() {
            GameInfo.Player judge = info.players.get(judgeIndex);
            ui.event(UiEvent.JUDGE_SKIPPED, judge.name);
        }

        public void removeFromHand(@NonNull BaseCard card) {
            handAdapter.removeCard(card);
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

        /**
         * @return Whether the exception has been handled
         */
        public boolean handleStartGameException(PyxException ex) {
            if (Objects.equals(ex.errorCode, "nep")) {
                Toaster.show(context, Utils.Messages.NOT_ENOUGH_PLAYERS);
                return true;
            } else if (Objects.equals(ex.errorCode, "nec")) {
                try {
                    listener.showDialog(Dialogs.notEnoughCards(context, ex));
                    return true;
                } catch (JSONException exx) {
                    Logging.log(exx);
                    return false;
                }
            } else {
                return false;
            }
        }

        public void showTableCards() {
            if (whiteCardsList.getAdapter() != data.tableAdapter)
                whiteCardsList.swapAdapter(data.tableAdapter, true);
        }

        public void showHandCards() {
            if (whiteCardsList.getAdapter() != data.handAdapter)
                whiteCardsList.swapAdapter(data.handAdapter, true);
        }

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

        public void judgeSelectCard(@NonNull final BaseCard card) {
            listener.showDialog(Dialogs.confirmation(context, new Dialogs.OnConfirmed() {
                @Override
                public void onConfirmed() {
                    judgeSelectCardInternal(card);
                }
            }));
        }

        public void playCard(@NonNull final BaseCard card) {
            if (card.writeIn()) {
                listener.showDialog(Dialogs.askText(context, new Dialogs.OnText() {
                    @Override
                    public void onText(@NonNull String text) {
                        playCardInternal(card, text);
                    }
                }));
            } else {
                listener.showDialog(Dialogs.confirmation(context, new Dialogs.OnConfirmed() {
                    @Override
                    public void onConfirmed() {
                        playCardInternal(card, null);
                    }
                }));
            }
        }

        //*****************//
        // Private methods //
        //*****************//

        private void judgeSelectCardInternal(@NonNull BaseCard card) {
            pyx.request(PyxRequests.judgeCard(gid(), card.id()), new Pyx.OnSuccess() {
                @Override
                public void onDone() {
                    AnalyticsApplication.sendAnalytics(context, Utils.ACTION_JUDGE_CARD);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    Toaster.show(context, Utils.Messages.FAILED_JUDGING, ex);
                }
            });
        }

        private void playCardInternal(@NonNull final BaseCard card, @Nullable final String customText) {
            pyx.request(PyxRequests.playCard(gid(), card.id(), customText), new Pyx.OnSuccess() {
                @Override
                public void onDone() {
                    data.removeFromHand(card);

                    if (customText == null)
                        AnalyticsApplication.sendAnalytics(context, Utils.ACTION_PLAY_CARD);
                    else
                        AnalyticsApplication.sendAnalytics(context, Utils.ACTION_PLAY_CUSTOM_CARD);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    Toaster.show(context, Utils.Messages.FAILED_PLAYING, ex);
                }
            });
        }

        private void uiToast(@NonNull String text, Object... args) {
            Toaster.show(context, String.format(Locale.getDefault(), text, args), Toast.LENGTH_SHORT, null, null, null);
        }

        private void uiText(@NonNull String text, Object... args) {
            instructions.setText(String.format(Locale.getDefault(), text, args));
        }

        @Nullable
        public BaseCard blackCard() {
            return blackCard.getCard();
        }

        public void setStartGameVisible(boolean set) {
            startGame.setVisibility(set ? View.VISIBLE : View.GONE);
        }
    }
}
