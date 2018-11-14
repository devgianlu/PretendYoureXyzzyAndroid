package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Dialogs.Dialogs;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

public class AnotherGameManager implements Pyx.OnEventListener, GameLayout.Listener, SensitiveGameData.Listener {
    private final RegisteredPyx pyx;
    private final GameLayout gameLayout;
    private final SensitiveGameData gameData;
    private final Listener listener;
    private final int gid;
    private final Context context;

    public AnotherGameManager(int gid, @NonNull RegisteredPyx pyx, @NonNull GameLayout layout, @NonNull Listener listener) {
        this.pyx = pyx;
        this.gid = gid;
        this.context = layout.getContext();
        this.gameLayout = layout;
        this.gameLayout.attach(this);
        this.gameData = new SensitiveGameData(pyx, this);
        this.listener = listener;
    }

    @Override
    public void onPollMessage(@NonNull PollMessage msg) throws JSONException {
        Logging.log("AnotherGameManager: " + msg.event + " -> " + msg.obj.toString(), false);

        switch (msg.event) {
            case GAME_JUDGE_LEFT:
                judgeLeft();
                break;
            case GAME_JUDGE_SKIPPED:
                judgeSkipped();
                break;
            case GAME_OPTIONS_CHANGED:
                gameData.update(new Game(msg.obj.getJSONObject("gi")));
                break;
            case GAME_PLAYER_INFO_CHANGE:
                gameData.playerChange(new GameInfo.Player(msg.obj.getJSONObject("pi")));
                break;
            case GAME_PLAYER_KICKED_IDLE:
                event(UiEvent.PLAYER_KICKED, msg.obj.getString("n"));
                break;
            case GAME_PLAYER_JOIN:
                if (!msg.obj.getString("n").equals(gameData.me)) updateGameInfo();
                break;
            case GAME_PLAYER_LEAVE:
                if (!msg.obj.getString("n").equals(gameData.me)) updateGameInfo();
                break;
            case GAME_PLAYER_SKIPPED:
                event(UiEvent.PLAYER_SKIPPED, msg.obj.getString("n"));
                break;
            case GAME_ROUND_COMPLETE:
                roundComplete(msg.obj.getInt("WC"), msg.obj.getString("rw"));
                break;
            case GAME_STATE_CHANGE:
                gameStateChange(msg);
                break;
            case HAND_DEAL:
                dealCards(Card.list(msg.obj.getJSONArray("h")));
                break;
            case KICKED_FROM_GAME_IDLE:
                destroy();
                break;
            case HURRY_UP:
                event(UiEvent.HURRY_UP);
                break;
            case GAME_SPECTATOR_JOIN:
                gameData.spectatorJoin(msg.obj.getString("n"));
                break;
            case GAME_SPECTATOR_LEAVE:
                gameData.spectatorLeave(msg.obj.getString("n"));
                break;
            case GAME_LIST_REFRESH:
            case GAME_BLACK_RESHUFFLE:
            case GAME_WHITE_RESHUFFLE:
            case KICKED:
            case BANNED:
            case CARDCAST_ADD_CARDSET:
            case CARDCAST_REMOVE_CARDSET:
            case CHAT:
            case PLAYER_LEAVE:
            case NEW_PLAYER:
            case NOOP:
                break;
        }
    }

    private void judgeSkipped() {
        if (gameData.judge != null)
            event(UiEvent.JUDGE_SKIPPED, gameData.judge);
    }

    private void judgeLeft() {
        if (gameData.judge != null)
            event(UiEvent.JUDGE_LEFT, gameData.judge);

        gameLayout.clearTable();
        gameLayout.showTable(false);
        gameLayout.setBlackCard(null);
    }

    private void roundComplete(int winnerCard, String roundWinner) {
        gameLayout.notifyWinnerCard(winnerCard);

        if (roundWinner.equals(gameData.me)) event(UiEvent.YOU_ROUND_WINNER);
        else event(UiEvent.ROUND_WINNER, roundWinner);
    }

    private void dealCards(List<Card> cards) {
        gameLayout.addHand(cards);
    }

    private void gameStateChange(@NonNull PollMessage msg) throws JSONException {
        Game.Status status = Game.Status.parse(msg.obj.getString("gs"));
        gameData.update(status);
        switch (status) {
            case JUDGING:
                gameLayout.setTable(CardsGroup.list(msg.obj.getJSONArray("wc")));
                gameLayout.showTable(gameData.amJudge());
                break;
            case PLAYING:
                gameLayout.clearTable();
                gameLayout.setBlackCard(new Card(msg.obj.getJSONObject("bc")));
                updateGameInfo();
                break;
            case LOBBY:
                event(UiEvent.WAITING_FOR_START);
                gameLayout.setBlackCard(null);
                gameLayout.clearTable();
                gameLayout.showTable(false);
                gameData.resetToIdleAndHost();
                break;
            case DEALING:
            case ROUND_OVER:
                break;
        }
    }

    @Override
    public void ourPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus) {
        gameLayout.startGameVisible(player.status == GameInfo.PlayerStatus.HOST);

        switch (player.status) {
            case HOST:
                event(UiEvent.YOU_GAME_HOST);
                break;
            case IDLE:
                if (oldStatus == GameInfo.PlayerStatus.PLAYING) {
                    if (gameData.status != Game.Status.JUDGING)
                        event(UiEvent.WAITING_FOR_OTHER_PLAYERS);
                } else if (oldStatus == null) {
                    if (gameData.status == Game.Status.LOBBY) event(UiEvent.WAITING_FOR_START);
                    else event(UiEvent.WAITING_FOR_ROUND_TO_END);
                }

                gameLayout.showTable(false);
                break;
            case JUDGE:
                if (oldStatus != GameInfo.PlayerStatus.JUDGING) event(UiEvent.YOU_JUDGE);
                gameLayout.showTable(true);
                break;
            case JUDGING:
                event(UiEvent.SELECT_WINNING_CARD);
                gameLayout.showTable(true);
                break;
            case PLAYING:
                BaseCard bc = gameLayout.blackCard();
                if (bc != null) event(UiEvent.PICK_CARDS, bc.numPick());
                gameLayout.showHand(true);
                break;
            case WINNER:
                event(UiEvent.YOU_GAME_WINNER);
                break;
            case SPECTATOR:
                break;
        }
    }

    @Override
    public void anyPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus) {
        if (player.status == GameInfo.PlayerStatus.HOST)
            listener.updateActivityTitle();
    }

    @Override
    public void notOutPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus) {
        if (player.status == GameInfo.PlayerStatus.WINNER)
            event(UiEvent.GAME_WINNER, player.name);

        if (player.status == GameInfo.PlayerStatus.JUDGING && gameData.status == Game.Status.JUDGING)
            event(UiEvent.IS_JUDGING, player.name);

        if (oldStatus == GameInfo.PlayerStatus.PLAYING && player.status == GameInfo.PlayerStatus.IDLE && gameData.status == Game.Status.PLAYING)
            gameLayout.addBlankCardTable();
    }

    @Override
    public void onStoppedPolling() {
        destroy();
        listener.justLeaveGame();
    }

    public void destroy() {
        pyx.polling().removeListener(this);
        listener.justLeaveGame();
    }

    public boolean amHost() {
        return gameData.amHost();
    }

    public void refresh() {
        // TODO: Refresh
    }

    private void updateGameInfo() {
        pyx.request(PyxRequests.getGameInfo(gid), new Pyx.OnResult<GameInfo>() {
            @Override
            public void onDone(@NonNull GameInfo result) {
                gameData.update(result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.showToast(Toaster.build().message(R.string.failedLoading).ex(ex));
            }
        });
    }

    public void begin() {
        pyx.polling().addListener(this);

        pyx.getGameInfoAndCards(gid, new Pyx.OnResult<GameInfoAndCards>() {
            @Override
            public void onDone(@NonNull GameInfoAndCards result) {
                gameData.update(result.info, result.cards, gameLayout);
                listener.onGameLoaded();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailedLoadingGame(ex);
            }
        });
    }

    @Override
    public void onCardSelected(@NonNull final BaseCard card) {
        if (gameData.amJudge()) {
            pyx.request(PyxRequests.judgeCard(gid, card.id()), new Pyx.OnSuccess() {
                @Override
                public void onDone() {
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    if (ex instanceof PyxException) {
                        if (((PyxException) ex).errorCode.equals("nj")) {
                            event(UiEvent.NOT_YOUR_TURN);
                            return;
                        }
                    }

                    listener.showToast(Toaster.build().message(R.string.failedJudging).ex(ex));
                }
            });
        } else {
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
    }

    private void playCardInternal(@NonNull final BaseCard card, @Nullable String text) {
        pyx.request(PyxRequests.playCard(gid, card.id(), text), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                gameLayout.removeHand(card);
                gameLayout.addTable(card); // FIXME: Doesn't work if pick >= 2
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (ex instanceof PyxException) {
                    if (((PyxException) ex).errorCode.equals("nyt")) {
                        event(UiEvent.NOT_YOUR_TURN);
                        return;
                    }
                }

                listener.showToast(Toaster.build().message(R.string.failedPlayingCard).ex(ex));
            }
        });
    }

    public boolean isStatus(@NonNull Game.Status status) {
        return gameData.status == status;
    }

    @NonNull
    public List<GameInfo.Player> players() {
        return Collections.unmodifiableList(gameData.players);
    }

    @NonNull
    public Collection<String> spectators() {
        return Collections.unmodifiableCollection(gameData.spectators);
    }

    @NonNull
    public Game.Options gameOptions() {
        return gameData.options;
    }

    @NonNull
    public String host() {
        return gameData.host;
    }

    public void event(@NonNull UiEvent ev, Object... args) {
        switch (ev.kind) {
            case BOTH:
                listener.showToast(Toaster.build().message(ev.toast, args).error(false));
                if (ev == UiEvent.SPECTATOR_TEXT || !gameData.amSpectator())
                    gameLayout.setInstructions(ev.text, args);
                break;
            case TOAST:
                listener.showToast(Toaster.build().message(ev.toast, args).error(false));
                break;
            case TEXT:
                if (ev == UiEvent.SPECTATOR_TEXT || !gameData.amSpectator())
                    gameLayout.setInstructions(ev.text, args);
                break;
        }
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
        SPECTATOR_TOAST(R.string.game_spectator, Kind.TOAST); // TODO: Test spectator mode

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
        void onGameLoaded();

        void onFailedLoadingGame(@NonNull Exception ex);

        void updateActivityTitle();

        void showToast(@NonNull Toaster toaster);

        void showDialog(@NonNull AlertDialog.Builder dialog);

        void justLeaveGame();
    }
}
