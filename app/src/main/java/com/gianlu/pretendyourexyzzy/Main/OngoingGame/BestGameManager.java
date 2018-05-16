package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
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
                data.gameOptionsChanged(new GameInfo(msg.obj.getJSONObject("gi")));
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
                listener.shouldLeaveGame();
                break;
            case GAME_SPECTATOR_JOIN:
                data.gameSpectatorJoin(msg.obj.getString("n"));
                break;
            case GAME_SPECTATOR_LEAVE:
                data.gameSpectatorLeave(msg.obj.getString("n"));
                break;
            case KICKED:
            case BANNED:
                listener.shouldLeaveGame();
                break;
            case GAME_BLACK_RESHUFFLE:
            case GAME_WHITE_RESHUFFLE:
                break;
            case CARDCAST_REMOVE_CARDSET:
            case CARDCAST_ADD_CARDSET:
                break;
            case CHAT:
            case GAME_LIST_REFRESH:
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

    private enum UiEvent {
        YOU_JUDGE(R.string.game_youJudge, Kind.TEXT),
        SELECT_WINNING_CARD(R.string.game_selectWinningCard, Kind.TEXT),
        YOU_ROUND_WINNER(R.string.game_youRoundWinner_long, R.string.game_youRoundWinner_short),
        SPECTATOR_TEXT(R.string.game_spectator, Kind.TEXT),
        YOU_GAME_HOST(R.string.game_youGameHost, Kind.TEXT),
        WAITING_FOR_ROUND_TO_END(R.string.game_waitingForRoundToEnd, Kind.TEXT),
        WAITING_FOR_START(R.string.game_waitingForStart, Kind.TEXT),
        JUDGE_LEFT(R.string.game_judgeLeft_long, R.string.game_judgeLeft_short),
        IS_JUDGING(R.string.game_isJudging, Kind.TEXT),
        ROUND_WINNER(R.string.game_roundWinner_long, R.string.game_roundWinner_short),
        WAITING_FOR_OTHER_PLAYERS(R.string.game_waitingForPlayers, Kind.TEXT),
        PLAYER_SKIPPED(R.string.game_playerSkipped, Kind.TOAST),
        PICK_CARDS(R.string.game_pickCards, Kind.TEXT),
        JUDGE_SKIPPED(R.string.game_judgeSkipped, Kind.TOAST),
        GAME_WINNER(R.string.game_gameWinner_long, R.string.game_gameWinner_short),
        YOU_GAME_WINNER(R.string.game_youGameWinner_long, R.string.game_youGameWinner_short),
        NOT_YOUR_TURN(R.string.game_notYourTurn, Kind.TOAST),
        HURRY_UP(R.string.hurryUp, Kind.TOAST),
        PLAYER_KICKED(R.string.game_playerKickedIdle, Kind.TOAST),
        SPECTATOR_TOAST(R.string.game_spectator, Kind.TOAST);

        private final int toast;
        private final int text;
        private final Kind kind;

        UiEvent(@StringRes int text, Kind kind) {
            this.text = text;
            this.kind = kind;
            this.toast = 0;
        }

        UiEvent(@StringRes int text, @StringRes int toast) {
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
        void shouldLeaveGame();

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

            playersAdapter = new PlayersAdapter(context, info.players);
            ui.playersList.setAdapter(playersAdapter);

            handAdapter = new CardsAdapter(context, true, PyxCardsGroupView.Action.TOGGLE_STAR, this);
            handAdapter.setCards(cards.hand);

            tableAdapter = new CardsAdapter(context, true, PyxCardsGroupView.Action.TOGGLE_STAR, this);
            tableAdapter.setCardGroups(cards.whiteCards);

            ui.blackCard(cards.blackCard);
        }

        public void setup() { // Called ONLY from constructor
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

            if (info.game.spectators.contains(me())) {
                ui.showTableCards();
                ui.event(UiEvent.SPECTATOR_TEXT);
            } else {
                GameInfo.Player me = info.player(me());
                if (me != null) {
                    switch (me.status) {
                        case JUDGE:
                            ui.event(UiEvent.YOU_JUDGE);
                            ui.showTableCards();
                            break;
                        case JUDGING:
                            ui.event(UiEvent.SELECT_WINNING_CARD);
                            ui.showTableCards();
                            break;
                        case PLAYING:
                            BaseCard bc = ui.blackCard();
                            if (bc != null) ui.event(UiEvent.PICK_CARDS, bc.numPick());
                            ui.showHandCards();
                            break;
                        case IDLE:
                            ui.showTableCards();

                            if (info.game.status == Game.Status.JUDGING) {
                                GameInfo.Player judge = info.players.get(judgeIndex);
                                ui.event(UiEvent.IS_JUDGING, judge.name);
                            } else {
                                ui.event(UiEvent.WAITING_FOR_ROUND_TO_END);
                            }
                            break;
                        case WINNER:
                            ui.event(UiEvent.YOU_GAME_WINNER);
                            break;
                        case HOST:
                            ui.event(UiEvent.YOU_GAME_HOST);
                            break;
                        case SPECTATOR:
                            ui.showTableCards();
                            break;
                    }
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

                        if (info.game.status != Game.Status.JUDGING) // Called after #gameRoundComplete()
                            ui.event(UiEvent.YOU_JUDGE);
                    }

                    judgeIndex = Utils.indexOf(info.players, player.name); // Redundant, but for safety...
                    break;
                case IDLE:
                    if (Objects.equals(player.name, me())) {
                        ui.showTableCards();

                        if (info.game.status != Game.Status.JUDGING)
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
                case WINNER:
                    ui.event(UiEvent.YOU_GAME_WINNER);
                    break;
                case HOST:
                    ui.event(UiEvent.YOU_GAME_HOST);
                    break;
                case SPECTATOR:
                    ui.showTableCards();
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
        public void onCardAction(@NonNull PyxCardsGroupView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
            if (action == PyxCardsGroupView.Action.SELECT) {
                GameInfo.Player me = info.player(me());
                if (me != null) {
                    if (me.status == GameInfo.PlayerStatus.PLAYING && info.game.status == Game.Status.PLAYING) {
                        ui.playCard(card);
                    } else if ((me.status == GameInfo.PlayerStatus.JUDGE || me.status == GameInfo.PlayerStatus.JUDGING) && info.game.status == Game.Status.JUDGING) {
                        ui.judgeSelectCard(card);
                    } else {
                        if (info.game.spectators.contains(me())) {
                            ui.event(UiEvent.SPECTATOR_TOAST);
                        } else {
                            ui.event(UiEvent.NOT_YOUR_TURN);
                        }
                    }
                }
            } else if (action == PyxCardsGroupView.Action.TOGGLE_STAR) {
                BaseCard bc = ui.blackCard();
                if (bc != null && StarredCardsManager.addCard(context, new StarredCardsManager.StarredCard(bc, group)))
                    Toaster.show(context, Utils.Messages.STARRED_CARD);
            }
        }

        public void gameOptionsChanged(@NonNull GameInfo info) { // TODO: May be useful
            this.info.game.options = info.game.options;
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

        public void gameSpectatorJoin(String nick) {
            info.newSpectator(nick);
        }

        public void gameSpectatorLeave(String nick) {
            info.removeSpectator(nick);
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

        private void uiToast(int text, Object... args) {
            Toaster.show(context, context.getString(text, args), Toast.LENGTH_SHORT, null, null, null);
        }

        private void uiText(int text, Object... args) {
            instructions.setText(context.getString(text, args));
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
