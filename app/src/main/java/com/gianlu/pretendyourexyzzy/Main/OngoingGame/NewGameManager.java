package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gianlu.commonutils.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.Cards.PyxCard;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.LoadingActivity;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.NetIO.PYXException;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NewGameManager implements PYX.IEventListener, CardsAdapter.IAdapter {
    private final Context context;
    private final FloatingActionButton startGame;
    private final PyxCard blackCard;
    private final TextView instructions;
    private final PYX pyx;
    private final PlayersAdapter playersAdapter;
    private final CardsAdapter handAdapter;
    private final CardsAdapter tableCardsAdapter;
    private final RecyclerView whiteCardsList;
    private final User me;
    public GameInfo gameInfo;
    private GameInfo.PlayerStatus myLastStatus;
    private boolean playedForLast = false;

    public NewGameManager(Context context, ViewGroup layout, User me, GameInfo gameInfo, GameCards cards) {
        this.context = context;
        this.me = me;
        this.gameInfo = gameInfo;
        this.pyx = PYX.get(context);
        this.pyx.getPollingThread().addListener("gameManager", this);

        startGame = layout.findViewById(R.id.gameLayout_startGame);
        instructions = layout.findViewById(R.id.gameLayout_instructions);
        blackCard = layout.findViewById(R.id.gameLayout_blackCard);
        blackCard.setCard(cards.blackCard);

        RecyclerView playersList = layout.findViewById(R.id.gameLayout_players);
        playersList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        playersAdapter = new PlayersAdapter(context, gameInfo.players);
        playersList.setAdapter(playersAdapter);

        whiteCardsList = layout.findViewById(R.id.gameLayout_whiteCards);
        whiteCardsList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        handAdapter = new CardsAdapter(context, cards.hand, this);
        tableCardsAdapter = new CardsAdapter(context, this);
        tableCardsAdapter.notifyItemInserted(cards.whiteCards);

        GameInfo.Player alsoMe = Utils.find(gameInfo.players, me.nickname);
        if (alsoMe != null) handleMyStatusChange(alsoMe.status);
        else handleMyStatusChange(GameInfo.PlayerStatus.SPECTATOR);

        isLobby(gameInfo.game.status == Game.Status.LOBBY && Objects.equals(gameInfo.game.host, me.nickname));

        if (myLastStatus == GameInfo.PlayerStatus.IDLE) {
            switch (gameInfo.game.status) {
                case DEALING:
                case ROUND_OVER:
                    break;
                case JUDGING:
                    GameInfo.Player judge = findJudge();
                    if (judge != null) updateInstructions(Instructions.IS_JUDGING(judge.name));
                    break;
                case LOBBY:
                    updateInstructions(Instructions.WAITING_FOR_START);
                    break;
                case PLAYING:
                    updateInstructions(Instructions.WAITING_FOR_ROUND_TO_END);
                    break;
            }
        }
    }

    private void startGame() {
        pyx.startGame(gameInfo.game.gid, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                Toaster.show(context, Utils.Messages.GAME_STARTED);
            }

            @Override
            public void onException(Exception ex) {
                if (ex instanceof PYXException && !cannotStartGameDialog((PYXException) ex))
                    Toaster.show(context, Utils.Messages.FAILED_START_GAME, ex);
            }
        });
    }

    @SuppressWarnings("InflateParams")
    private boolean cannotStartGameDialog(PYXException ex) {
        if (Objects.equals(ex.errorCode, "nep")) {
            Toaster.show(context, Utils.Messages.NOT_ENOUGH_PLAYERS);
            return true;
        } else if (Objects.equals(ex.errorCode, "nec")) {
            int wcr;
            int bcr;
            int wcp;
            int bcp;
            try {
                wcr = ex.obj.getInt("wcr");
                bcr = ex.obj.getInt("bcr");
                wcp = ex.obj.getInt("wcp");
                bcp = ex.obj.getInt("bcp");
            } catch (JSONException exx) {
                Logging.logMe(context, exx);
                return true;
            }

            LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.cannot_start_game_dialog, null, false);
            ((TextView) layout.findViewById(R.id.cannotStartGame_wcr)).setText(String.valueOf(wcr));
            ((TextView) layout.findViewById(R.id.cannotStartGame_bcr)).setText(String.valueOf(bcr));
            ((TextView) layout.findViewById(R.id.cannotStartGame_wcp)).setText(String.valueOf(wcp));
            ((TextView) layout.findViewById(R.id.cannotStartGame_bcp)).setText(String.valueOf(bcp));
            ((ImageView) layout.findViewById(R.id.cannotStartGame_checkBc)).setImageResource(bcp >= bcr ? R.drawable.ic_done_black_48dp : R.drawable.ic_clear_black_48dp);
            ((ImageView) layout.findViewById(R.id.cannotStartGame_checkWc)).setImageResource(wcp >= wcr ? R.drawable.ic_done_black_48dp : R.drawable.ic_clear_black_48dp);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.cannotStartGame)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok, null);

            CommonUtils.showDialog(context, builder);

            return false;
        } else {
            return true;
        }
    }

    private void isLobby(boolean lobby) {
        if (lobby) {
            startGame.setVisibility(View.VISIBLE);
            startGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (gameInfo.game.status == Game.Status.LOBBY) startGame();
                }
            });
        } else {
            startGame.setVisibility(View.GONE);
        }
    }

    private void updateInstructions(String text) {
        System.out.println("CHANGING TEXT TO: " + text); // FIXME
        instructions.setText(text);
    }

    private void tableCardsChanged(List<List<Card>> cards) {
        tableCardsAdapter.notifyDataSetChanged(cards);
    }

    private void handCardsChanged(List<Card> cards) {
        List<List<Card>> newCards = new ArrayList<>();
        for (Card card : cards) newCards.add(Collections.singletonList(card));

        if (cards.size() == 10) handAdapter.notifyDataSetChanged(newCards);
        else handAdapter.notifyItemInserted(newCards);
    }

    private void setBlackCard(@Nullable Card card) {
        blackCard.setCard(card);
        tableCardsAdapter.setAssociatedBlackCard(card);
    }

    @Nullable
    private GameInfo.Player findJudge() {
        for (GameInfo.Player player : gameInfo.players)
            if (player.status == GameInfo.PlayerStatus.JUDGING || player.status == GameInfo.PlayerStatus.JUDGE)
                return player;

        return null;
    }

    private void handleGameStatusChange(Game.Status newStatus, PollMessage message) throws JSONException {
        gameInfo.game.setStatus(newStatus);
        switch (newStatus) {
            case JUDGING:
                if (myLastStatus != GameInfo.PlayerStatus.SPECTATOR) {
                    GameInfo.Player judge = findJudge();
                    if (judge != null) updateInstructions(Instructions.IS_JUDGING(judge.name));
                }

                tableCardsChanged(GameCards.toWhiteCardsList(message.obj.getJSONArray("wc")));
                break;
            case LOBBY:
                setBlackCard(null);
                tableCardsChanged(new ArrayList<List<Card>>());
                handCardsChanged(new ArrayList<Card>());
                break;
            case PLAYING:
                setBlackCard(new Card(message.obj.getJSONObject("bc")));
                tableCardsChanged(new ArrayList<List<Card>>());
                refreshPlayersList();
                break;
        }
    }

    private void refreshPlayersList() {
        pyx.getGameInfo(gameInfo.game.gid, new PYX.IResult<GameInfo>() {
            @Override
            public void onDone(PYX pyx, GameInfo result) {
                gameInfo = result;
                playersAdapter.notifyDataSetChanged(result.players);

                for (GameInfo.Player player : result.players)
                    if (Objects.equals(player.name, me.nickname))
                        handleMyStatusChange(player.status);
            }

            @Override
            public void onException(Exception ex) {
                Logging.logMe(context, ex);
            }
        });
    }

    private void addWhiteCard() {
        if (gameInfo.game.status == Game.Status.PLAYING) tableCardsAdapter.addBlankCard();
    }

    private void handlePlayerInfoChange(GameInfo.Player player) {
        playersAdapter.notifyItemChanged(player);
        gameInfo.notifyPlayerChanged(player);

        if (player.status == GameInfo.PlayerStatus.IDLE) addWhiteCard();
        if (Objects.equals(player.name, me.nickname)) handleMyStatusChange(player.status);
    }

    private void removeCardFromHand(Card card) {
        handAdapter.notifyItemRemoved(card);
    }

    private void handleWinner(String winner, int winningCard) {
        tableCardsAdapter.notifyWinningCard(winningCard);
        Toaster.show(context, Instructions.THIS_GUY_WON_TOAST(winner), Toast.LENGTH_SHORT, null, null, null);
    }

    private void handleMyStatusChange(GameInfo.PlayerStatus newStatus) {
        myLastStatus = newStatus;

        switch (newStatus) {
            case HOST:
                updateInstructions(Instructions.GAME_HOST);
                break;
            case IDLE:
                if (!playedForLast) {
                    updateInstructions(Instructions.WAITING_FOR_OTHER_PLAYERS);
                    playedForLast = false;
                }

                if (whiteCardsList.getAdapter() != tableCardsAdapter)
                    whiteCardsList.swapAdapter(tableCardsAdapter, true);
                break;
            case JUDGE:
                if (gameInfo.game.status == Game.Status.PLAYING)
                    updateInstructions(Instructions.JUDGE);

                if (whiteCardsList.getAdapter() != tableCardsAdapter)
                    whiteCardsList.swapAdapter(tableCardsAdapter, true);
                break;
            case JUDGING:
                updateInstructions(Instructions.SELECT_WINNING_CARD);

                if (whiteCardsList.getAdapter() != tableCardsAdapter)
                    whiteCardsList.swapAdapter(tableCardsAdapter, true);
                break;
            case PLAYING:
                BaseCard blackCard = this.blackCard.getCard();
                if (blackCard != null)
                    updateInstructions(Instructions.PICK_CARDS(blackCard.getNumPick()));

                if (whiteCardsList.getAdapter() != handAdapter)
                    whiteCardsList.swapAdapter(handAdapter, true);
                break;
            case WINNER:
                updateInstructions(Instructions.WINNER);
                break;
            case SPECTATOR:
                updateInstructions(Instructions.SPECTATOR);

                if (whiteCardsList.getAdapter() != tableCardsAdapter)
                    whiteCardsList.swapAdapter(tableCardsAdapter, true);
                break;
        }
    }

    private void setPlayerSkipped(String nickname) {
        GameInfo.Player player = Utils.find(gameInfo.players, nickname);
        if (player != null) {
            GameInfo.Player newPlayer = new GameInfo.Player(player.name, player.score, GameInfo.PlayerStatus.IDLE);
            playersAdapter.notifyItemChanged(newPlayer);
            gameInfo.notifyPlayerChanged(newPlayer);
        }
    }

    @Override
    public void onPollMessage(PollMessage message) throws JSONException {
        if (message.event != PollMessage.Event.CHAT && BuildConfig.DEBUG)
            System.out.println("Event: " + message.event.name() + " -> " + message.obj);

        switch (message.event) {
            case BANNED:
            case KICKED:
            case NOOP:
            case CHAT:
            case GAME_BLACK_RESHUFFLE:
            case GAME_SPECTATOR_JOIN:
            case CARDCAST_REMOVE_CARDSET:
            case CARDCAST_ADD_CARDSET:
            case GAME_LIST_REFRESH:
            case GAME_WHITE_RESHUFFLE:
            case GAME_SPECTATOR_LEAVE:
            case NEW_PLAYER:
            case PLAYER_LEAVE:
                // Not interested in these
                return;
            case GAME_JUDGE_LEFT:
                /*
                updatePlayersCards(new ArrayList<List<Card>>());
                updateInstructions("Waiting for new round to start...");
                newBlackCard(null);
                if (listener != null) listener.showToast(new Toaster.Message(R.string.judgeLeft, false));
                */
                return;
            case GAME_OPTIONS_CHANGED:
                gameInfo = new GameInfo(new Game(message.obj.getJSONObject("gi")), gameInfo.players);
                break;
            case GAME_PLAYER_INFO_CHANGE:
                handlePlayerInfoChange(new GameInfo.Player(message.obj.getJSONObject("pi")));
                break;
            case GAME_PLAYER_JOIN:
                handlePlayerJoin(message.obj.getString("n"));
                break;
            case GAME_PLAYER_LEAVE:
            case GAME_PLAYER_KICKED_IDLE:
                handlePlayerLeave(message.obj.getString("n"));
                break;
            case GAME_PLAYER_SKIPPED:
                setPlayerSkipped(message.obj.getString("n"));
                // if (listener != null) listener.notifyPlayerSkipped(message.obj.getString("n"));
                break;
            case GAME_JUDGE_SKIPPED:
                // if (listener != null) listener.notifyJudgeSkipped(message.obj.optString("n", null));
                break;
            case GAME_ROUND_COMPLETE:
                String winner = message.obj.getString("rw");
                handleWinner(winner, message.obj.getInt("WC") /* intermission: message.obj.getInt("i") */);
                updateInstructions(Instructions.WINNER_AND_NEW_ROUND(winner));
                if (Objects.equals(winner, me.nickname))
                    handleMyStatusChange(GameInfo.PlayerStatus.WINNER);
                break;
            case GAME_STATE_CHANGE:
                handleGameStatusChange(Game.Status.parse(message.obj.getString("gs")), message);
                break;
            case HAND_DEAL:
                handCardsChanged(CommonUtils.toTList(message.obj.getJSONArray("h"), Card.class));
                handleMyStatusChange(GameInfo.PlayerStatus.PLAYING);
                break;
            case HURRY_UP:
                // if (listener != null) listener.showToast(Utils.Messages.HURRY_UP);
                break;
            case KICKED_FROM_GAME_IDLE:
                // if (listener != null) listener.kicked();
                break;
        }
    }

    private void handlePlayerJoin(String nickname) {
        playersAdapter.notifyItemInserted(new GameInfo.Player(nickname, 0, GameInfo.PlayerStatus.IDLE));
    }

    private void handlePlayerLeave(String nickname) {
        playersAdapter.notifyItemRemoved(nickname);
    }

    @Override
    public void onStoppedPolling() {
        Toaster.show(context, Utils.Messages.FAILED_LOADING);
        context.startActivity(new Intent(context, LoadingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private void setAmLastPlaying() {
        for (GameInfo.Player player : gameInfo.players) {
            if (!Objects.equals(player.name, me.nickname) && player.status == GameInfo.PlayerStatus.PLAYING) {
                playedForLast = false;
                return;
            }
        }

        playedForLast = true;
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return whiteCardsList;
    }

    private void playCard(final Card card, @Nullable final String customText) {
        pyx.playCard(gameInfo.game.gid, card.id, customText, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                removeCardFromHand(card);
                setAmLastPlaying();

                HitBuilders.EventBuilder event = new HitBuilders.EventBuilder()
                        .setCategory(Utils.CATEGORY_USER_INPUT);

                if (customText != null) event.setAction(Utils.ACTION_PLAY_CUSTOM_CARD);
                else event.setAction(Utils.ACTION_PLAY_CARD);

                AnalyticsApplication.sendAnalytics(context, event);
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(context, Utils.Messages.FAILED_PLAYING, ex);
            }
        });
    }

    @Override
    public void onCardSelected(final BaseCard card) {
        if (myLastStatus == GameInfo.PlayerStatus.PLAYING) {
            if (card.isWriteIn()) {
                final EditText customText = new EditText(context);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.setBlankCardText)
                        .setView(customText)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                playCard((Card) card, customText.getText().toString());
                            }
                        });

                CommonUtils.showDialog(context, builder);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.areYouSurePlayCard)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                playCard((Card) card, null);
                            }
                        });

                CommonUtils.showDialog(context, builder);
            }
        } else if (myLastStatus == GameInfo.PlayerStatus.JUDGING || myLastStatus == GameInfo.PlayerStatus.JUDGE) {
            pyx.judgeCard(gameInfo.game.gid, ((Card) card).id, new PYX.ISuccess() {
                @Override
                public void onDone(PYX pyx) {
                    AnalyticsApplication.sendAnalytics(context, new HitBuilders.EventBuilder()
                            .setCategory(Utils.CATEGORY_USER_INPUT)
                            .setAction(Utils.ACTION_JUDGE_CARD));
                }

                @Override
                public void onException(Exception ex) {
                    Toaster.show(context, Utils.Messages.FAILED_JUDGING, ex);
                }
            });
        }
    }

    @Override
    public void onDeleteCard(StarredCardsManager.StarredCard card) {
        // Never called
    }

    private static final class Instructions {
        static final String WAITING_FOR_OTHER_PLAYERS = "Waiting for other players...";
        static final String JUDGE = "You're the Card Czar! Wait for other players...";
        static final String SELECT_WINNING_CARD = "Select the winning card(s).";
        static final String WINNER = "You won this round! A new round will begin shortly.";
        static final String SPECTATOR = "You're a spectator.";
        static final String GAME_HOST = "You're the game host! Start the game when you're ready.";
        static final String WAITING_FOR_ROUND_TO_END = "Waiting for the current round to end...";
        static final String WAITING_FOR_START = "Waiting for the game to start...";

        static String PICK_CARDS(int numPick) {
            if (numPick == 1) return "Select one card to play. Your hand: ";
            else return "Select " + numPick + " cards to play. Your hand:";
        }

        static String IS_JUDGING(String name) {
            return name + " is judging...";
        }

        static String WINNER_AND_NEW_ROUND(String winner) {
            return winner + " won this round. A new round will begin shortly.";
        }

        static String THIS_GUY_WON_TOAST(String winner) {
            return winner + " won this round!";
        }
    }
}
