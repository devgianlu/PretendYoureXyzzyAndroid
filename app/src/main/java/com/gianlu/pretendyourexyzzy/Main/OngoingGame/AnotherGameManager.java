package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import com.gianlu.commonutils.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONException;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class AnotherGameManager implements Pyx.OnEventListener, GameLayout.Listener, SensitiveGameData.Listener {
    private final RegisteredPyx pyx;
    private final GameLayout gameLayout;
    private final SensitiveGameData gameData;
    private final Listener listener;
    private final int gid;

    public AnotherGameManager(int gid, @NonNull RegisteredPyx pyx, @NonNull GameLayout layout, @NonNull Listener listener) {
        this.pyx = pyx;
        this.gid = gid;
        this.gameLayout = layout;
        this.gameLayout.attach(this);
        this.gameData = new SensitiveGameData(gid, pyx, this);
        this.listener = listener;
    }

    @Override
    public void onPollMessage(@NonNull PollMessage msg) throws JSONException {
        Logging.log(msg.event + " -> " + msg.obj.toString(), false);

        switch (msg.event) {
            case GAME_JUDGE_LEFT:
                break;
            case GAME_JUDGE_SKIPPED:
                break;
            case GAME_OPTIONS_CHANGED:
                gameData.update(new Game(msg.obj.getJSONObject("gi")));
                break;
            case GAME_PLAYER_INFO_CHANGE:
                gameData.playerChange(new GameInfo.Player(msg.obj.getJSONObject("pi")));
                break;
            case GAME_PLAYER_KICKED_IDLE:
                break;
            case GAME_PLAYER_JOIN:
                updateGameInfo();
                break;
            case GAME_PLAYER_LEAVE:
                updateGameInfo();
                break;
            case GAME_PLAYER_SKIPPED:
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
                break;
            case HURRY_UP:
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

    private void roundComplete(int winnerCard, String roundWinner) {
        gameLayout.notifyWinnerCard(winnerCard);
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
                gameLayout.setBlackCard(null);
                gameLayout.clearTable();
                gameLayout.showTable(false);
                break;
            case DEALING:
            case ROUND_OVER:
                break;
        }
    }

    @Override
    public void ourPlayerChanged(@NonNull GameInfo.Player player) {
        gameLayout.startGameVisible(player.status == GameInfo.PlayerStatus.HOST);

        switch (player.status) {
            case HOST:
                break;
            case IDLE:
                gameLayout.showTable(false);
                break;
            case JUDGE:
            case JUDGING:
                gameLayout.showTable(true);
                break;
            case PLAYING:
                gameLayout.showHand(true);
                break;
            case WINNER:
                break;
            case SPECTATOR:
                break;
        }
    }

    @Override
    public void anotherPlayerPlayed() {
        gameLayout.addBlankCardTable();
    }

    @Override
    public void onStoppedPolling() {
        destroy();
        // TODO: Leave application
    }

    public void destroy() {
        pyx.polling().removeListener(this);
        // TODO
    }

    public boolean amHost() {
        return gameData.amHost();
    }

    public void refresh() {
        // TODO
    }

    private void updateGameInfo() {
        pyx.request(PyxRequests.getGameInfo(gid), new Pyx.OnResult<GameInfo>() {
            @Override
            public void onDone(@NonNull GameInfo result) {
                gameData.update(result, null);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                ex.printStackTrace(); // TODO
            }
        });
    }

    public void begin() {
        pyx.polling().addListener(this);

        pyx.getGameInfoAndCards(gid, new Pyx.OnResult<GameInfoAndCards>() {
            @Override
            public void onDone(@NonNull GameInfoAndCards result) {
                listener.onGameLoaded();

                gameData.update(result.info, gameLayout);

                gameLayout.addHand(result.cards.hand);
                gameLayout.setBlackCard(result.cards.blackCard);
                gameLayout.setTable(result.cards.whiteCards);
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
                    ex.printStackTrace(); // TODO
                }
            });
        } else {
            pyx.request(PyxRequests.playCard(gid, card.id(), null /* FIXME */), new Pyx.OnSuccess() {
                @Override
                public void onDone() {
                    gameLayout.removeHand(card);
                    gameLayout.addTable(card);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    ex.printStackTrace(); // TODO
                }
            });
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
        void onGameLoaded();

        void onFailedLoadingGame(@NonNull Exception ex);
    }
}
