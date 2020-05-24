package com.gianlu.pretendyourexyzzy.main.ongoinggame;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.GameInfoAndCards;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.GameCard;
import com.gianlu.pretendyourexyzzy.dialogs.Dialogs;

import org.json.JSONException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.gianlu.commonutils.CommonUtils.optString;

public class AnotherGameManager implements Pyx.OnEventListener, GameLayout.Listener, SensitiveGameData.Listener {
    private static final String TAG = AnotherGameManager.class.getSimpleName();
    private final GamePermalink permalink;
    private final RegisteredPyx pyx;
    private final GameLayout gameLayout;
    private final SensitiveGameData gameData;
    private final Listener listener;
    private final int gid;
    private final Context context;
    private OnPlayerStateChanged playerStateListener = null;

    public AnotherGameManager(@NonNull GamePermalink permalink, @NonNull RegisteredPyx pyx, @NonNull GameLayout layout, @NonNull Listener listener) {
        this.permalink = permalink;
        this.pyx = pyx;
        this.gid = permalink.gid;
        this.context = layout.getContext();
        this.gameLayout = layout;
        this.gameLayout.attach(this);
        this.gameData = new SensitiveGameData(pyx, this);
        this.listener = listener;
    }

    @Override
    public void onPollMessage(@NonNull PollMessage msg) throws JSONException {
        Log.v(TAG, msg.event + " -> " + msg.obj.toString());

        switch (msg.event) {
            case GAME_JUDGE_LEFT:
                judgeLeft(msg.obj.getInt("i"));
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
            case GAME_PLAYER_LEAVE:
            case GAME_PLAYER_JOIN:
                if (!msg.obj.getString("n").equals(gameData.me)) updateGameInfo();
                break;
            case GAME_PLAYER_SKIPPED:
                event(UiEvent.PLAYER_SKIPPED, msg.obj.getString("n"));
                break;
            case GAME_ROUND_COMPLETE:
                roundComplete(msg.obj.getInt("WC"), msg.obj.getString("rw"), msg.obj.getInt("i"), optString(msg.obj, "rP"));
                break;
            case GAME_STATE_CHANGE:
                gameStateChange(msg);
                break;
            case HAND_DEAL:
                dealCards(GameCard.list(msg.obj.getJSONArray("h")));
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

    private void judgeLeft(int intermission) {
        if (gameData.judge != null)
            event(UiEvent.JUDGE_LEFT, gameData.judge);

        gameLayout.clearTable();
        gameLayout.showTable(false);
        gameLayout.setBlackCard(null);

        gameLayout.countFrom(intermission);
    }

    @Nullable
    public String getLastRoundMetricsId() {
        if (gameData.lastRoundPermalink == null) return null;
        String[] split = gameData.lastRoundPermalink.split("/");
        return split[split.length - 1];
    }

    private void roundComplete(int winnerCard, @NonNull String roundWinner, int intermission, @Nullable String lastRoundPermalink) {
        gameLayout.notifyWinnerCard(winnerCard);
        gameLayout.countFrom(intermission);

        if (roundWinner.equals(gameData.me)) {
            GPGamesHelper.incrementEvent(context, 1, GPGamesHelper.EVENT_ROUNDS_WON);
            GPGamesHelper.incrementAchievement(context, 1, GPGamesHelper.ACH_WIN_10_ROUNDS,
                    GPGamesHelper.ACH_WIN_30_ROUNDS, GPGamesHelper.ACH_WIN_69_ROUNDS,
                    GPGamesHelper.ACH_WIN_420_ROUNDS);

            event(UiEvent.YOU_ROUND_WINNER);
        } else {
            event(UiEvent.ROUND_WINNER, roundWinner);
        }

        gameData.lastRoundPermalink = lastRoundPermalink;
        if (!gameData.amSpectator())
            GPGamesHelper.incrementEvent(context, 1, GPGamesHelper.EVENT_ROUNDS_PLAYED);
    }

    private void dealCards(List<? extends BaseCard> cards) {
        gameLayout.addHand(cards);
    }

    private void gameStateChange(@NonNull PollMessage msg) throws JSONException {
        Game.Status status = Game.Status.parse(msg.obj.getString("gs"));
        gameData.update(status);
        switch (status) {
            case JUDGING:
                gameLayout.countFrom(msg.obj.getInt("Pt"));
                gameLayout.setTable(CardsGroup.list(msg.obj.getJSONArray("wc")), gameLayout.blackCard());
                gameLayout.showTable(gameData.amJudge());
                break;
            case PLAYING:
                gameLayout.countFrom(msg.obj.getInt("Pt"));
                gameLayout.clearTable();
                gameLayout.setBlackCard(GameCard.parse(msg.obj.getJSONObject("bc")));
                updateGameInfo();

                if (gameData.amHost()) {
                    boolean hasCardcast = false;
                    for (int deckId : gameData.options.cardSets) {
                        if (deckId < 0) {
                            hasCardcast = true; // Cardcast deck ids are always negative
                            break;
                        }
                    }

                    if (hasCardcast)
                        GPGamesHelper.unlockAchievement(context, GPGamesHelper.ACH_CARDCAST);
                }
                break;
            case LOBBY:
                event(UiEvent.WAITING_FOR_START);
                gameLayout.setBlackCard(null);
                gameLayout.clearTable();
                gameLayout.clearHand();
                gameLayout.showTable(false);
                gameData.resetToIdleAndHost();
                gameLayout.resetTimer();
                break;
            case DEALING:
            case ROUND_OVER:
                break;
        }

        permalink.gamePermalink = optString(msg.obj, "gp");
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
                GPGamesHelper.incrementEvent(context, 1, GPGamesHelper.EVENT_GAMES_WON);
                break;
            case SPECTATOR:
                break;
        }

        if (playerStateListener != null) playerStateListener.onPlayerStateChanged(player.status);
    }

    public boolean isPlayerStatus(@NonNull GameInfo.PlayerStatus status) {
        return gameData.isPlayerStatus(gameData.me, status);
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
    public void playerIsSpectator() {
        gameLayout.showTable(false);
        event(UiEvent.SPECTATOR_TEXT);
    }

    @Override
    public void onStoppedPolling() {
        destroy();
        listener.justLeaveGame();
    }

    public void reset() {
        pyx.polling().removeListener(this);
        gameLayout.resetTimer();
    }

    public void destroy() {
        reset();
        listener.justLeaveGame();
    }

    public boolean amHost() {
        return gameData.amHost();
    }

    private void updateGameInfo() {
        pyx.request(PyxRequests.getGameInfo(gid), null, new Pyx.OnResult<GameInfo>() {
            @Override
            public void onDone(@NonNull GameInfo result) {
                gameData.update(result);

                if (gameData.amHost()) {
                    int players = result.players.size() - 1; // Do not include ourselves
                    GPGamesHelper.achievementSteps(context, players, GPGamesHelper.ACH_3_PEOPLE_GAME,
                            GPGamesHelper.ACH_5_PEOPLE_GAME, GPGamesHelper.ACH_10_PEOPLE_GAME);
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed getting game info.", ex);
                listener.showToast(Toaster.build().message(R.string.failedLoading));
            }
        });
    }

    public void begin() {
        pyx.getGameInfoAndCards(gid, null, new Pyx.OnResult<GameInfoAndCards>() {
            @Override
            public void onDone(@NonNull GameInfoAndCards result) {
                gameData.update(result.info, result.cards, gameLayout);
                listener.onGameLoaded();

                pyx.polling().addListener(AnotherGameManager.this);
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
            listener.showDialog(Dialogs.confirmation(context, () -> judgeCardInternal(card)));
        } else {
            if (((GameCard) card).writeIn()) {
                listener.showDialog(Dialogs.askText(context, text -> playCardInternal(card, text)));
            } else {
                listener.showDialog(Dialogs.confirmation(context, () -> playCardInternal(card, null)));
            }
        }
    }

    @Override
    public void showDialog(@NonNull DialogFragment dialog) {
        listener.showDialog(dialog);
    }

    @Override
    public void startGame() {
        pyx.request(PyxRequests.startGame(gid), null, new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                Toaster.with(context).message(R.string.gameStarted).show();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed starting game.", ex);
                if (!(ex instanceof PyxException) || !handleStartGameException((PyxException) ex))
                    Toaster.with(context).message(R.string.failedStartGame).show();
            }
        });
    }

    /**
     * @return Whether the exception has been handled
     */
    public boolean handleStartGameException(@NonNull PyxException ex) {
        if (Objects.equals(ex.errorCode, "nep")) {
            Toaster.with(context).message(R.string.notEnoughPlayers).show();
            return true;
        } else if (Objects.equals(ex.errorCode, "nec")) {
            try {
                listener.showDialog(Dialogs.notEnoughCards(context, ex));
                return true;
            } catch (JSONException exx) {
                Log.e(TAG, "Failed parsing JSON.", exx);
                return false;
            }
        } else {
            return false;
        }
    }

    private void judgeCardInternal(@NonNull BaseCard card) {
        pyx.request(PyxRequests.judgeCard(gid, card.id()), null, new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                ThisApplication.sendAnalytics(Utils.ACTION_JUDGE_CARD);
                GPGamesHelper.incrementEvent(context, 1, GPGamesHelper.EVENT_ROUNDS_JUDGED);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (ex instanceof PyxException) {
                    if (((PyxException) ex).errorCode.equals("nj")) {
                        event(UiEvent.NOT_YOUR_TURN);
                        return;
                    }
                }

                Log.e(TAG, "Failed judging.", ex);
                listener.showToast(Toaster.build().message(R.string.failedJudging));
            }
        });
    }

    private void playCardInternal(@NonNull BaseCard card, @Nullable String text) {
        pyx.request(PyxRequests.playCard(gid, card.id(), text), null, new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                gameLayout.removeHand(card);
                gameLayout.addTable(card, gameLayout.blackCard());

                ThisApplication.sendAnalytics(text == null ? Utils.ACTION_PLAY_CARD : Utils.ACTION_PLAY_CUSTOM_CARD);
                GPGamesHelper.incrementEvent(context, 1, GPGamesHelper.EVENT_CARDS_PLAYED);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                if (ex instanceof PyxException) {
                    if (((PyxException) ex).errorCode.equals("nyt")) {
                        event(UiEvent.NOT_YOUR_TURN);
                        return;
                    } else if (((PyxException) ex).errorCode.equals("dnhc")) {
                        gameLayout.removeHand(card);
                    }
                }

                Log.e(TAG, "Failed playing.", ex);
                listener.showToast(Toaster.build().message(R.string.failedPlayingCard));
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

    @Nullable
    public Game.Options gameOptions() {
        return gameData.options;
    }

    @NonNull
    public String host() {
        return gameData.host;
    }

    private void event(@NonNull UiEvent ev, Object... args) {
        switch (ev.kind) {
            case BOTH:
                listener.showToast(Toaster.build().message(ev.toast, args));
                if (ev == UiEvent.SPECTATOR_TEXT || !gameData.amSpectator())
                    gameLayout.setInstructions(ev.text, args);
                break;
            case TOAST:
                listener.showToast(Toaster.build().message(ev.text, args));
                break;
            case TEXT:
                if (ev == UiEvent.SPECTATOR_TEXT || !gameData.amSpectator())
                    gameLayout.setInstructions(ev.text, args);
                break;
        }
    }

    public boolean hasPassword(boolean knowsPassword) {
        if (knowsPassword)
            return gameData.options.password != null && !gameData.options.password.isEmpty();
        else return gameData.hasPassword;
    }

    public void setPlayerStateChangedListener(OnPlayerStateChanged listener) {
        playerStateListener = listener;
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
        PLAYER_KICKED(R.string.game_playerKickedIdle, Kind.TOAST);

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

        void showDialog(@NonNull DialogFragment dialog);
    }

    public interface OnPlayerStateChanged {
        void onPlayerStateChanged(@NonNull GameInfo.PlayerStatus status);
    }
}
