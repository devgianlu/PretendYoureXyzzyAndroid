package com.gianlu.pretendyourexyzzy.NetIO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.annotation.NonNull;
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

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.Adapters.PlayersAdapter;
import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.Cards.PyxCard;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// FIXME: Blank cards sometimes appears at the end of the cards list
public class GameManager implements PYX.IResult<List<PollMessage>>, CardsAdapter.IAdapter {
    private final static String POLL_TAG = "gameManager";
    private final PyxCard blackCard;
    private final TextView instructions;
    private final RecyclerView whiteCards;
    private final Context context;
    private final PlayersAdapter playersAdapter;
    private final CardsAdapter playersCardsAdapter;
    private final CardsAdapter handAdapter;
    private final Handler handler;
    private final User me;
    private final IManager listener;
    private final PYX pyx;
    private final FloatingActionButton startGame;
    public GameInfo gameInfo;
    private GameInfo.PlayerStatus lastMineStatus;
    private boolean lastPlaying;

    public GameManager(ViewGroup gameLayout, @NonNull GameInfo gameInfo, User me, IManager listener) {
        this.context = gameLayout.getContext();
        this.gameInfo = gameInfo;
        this.me = me;
        this.listener = listener;
        this.handler = new Handler(context.getMainLooper());
        this.pyx = PYX.get(context);
        pyx.getPollingThread().addListener(POLL_TAG, this);

        startGame = gameLayout.findViewById(R.id.gameLayout_startGame);
        blackCard = gameLayout.findViewById(R.id.gameLayout_blackCard);
        RecyclerView playersList = gameLayout.findViewById(R.id.gameLayout_players);
        playersList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        playersAdapter = new PlayersAdapter(context, gameInfo.players);
        playersList.setAdapter(playersAdapter);

        instructions = gameLayout.findViewById(R.id.gameLayout_instructions);
        whiteCards = gameLayout.findViewById(R.id.gameLayout_whiteCards);
        whiteCards.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        playersCardsAdapter = new CardsAdapter(context, this);
        handAdapter = new CardsAdapter(context, this);
        whiteCards.setAdapter(playersCardsAdapter);

        GameInfo.Player alsoMe = Utils.find(gameInfo.players, me.nickname);
        if (alsoMe != null) handleMyStatus(alsoMe.status);
        isLobby(gameInfo.game.status == Game.Status.LOBBY);
    }

    private void isLobby(boolean lobby) {
        if (lobby && Objects.equals(gameInfo.game.host, me.nickname)) {
            handleMyStatus(GameInfo.PlayerStatus.HOST);
            startGame.setVisibility(View.VISIBLE);
            startGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startGame();
                }
            });
        } else {
            startGame.setVisibility(View.GONE);
        }
    }

    private void startGame() {
        pyx.startGame(gameInfo.game.gid, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                if (listener != null) listener.showToast(Utils.Messages.GAME_STARTED);
            }

            @Override
            public void onException(Exception ex) {
                if (ex instanceof PYXException) {
                    cannotStartGame((PYXException) ex);
                }

                if (listener != null) listener.cannotStartGame(ex);
            }
        });
    }

    @SuppressLint("InflateParams")
    private void cannotStartGame(PYXException ex) {
        if (Objects.equals(ex.errorCode, "nep")) {
            if (listener != null) listener.showToast(Utils.Messages.NOT_ENOUGH_PLAYERS);
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
                return;
            }

            LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.cannot_start_game_dialog, null, false);
            TextView wcrText = layout.findViewById(R.id.cannotStartGame_wcr);
            wcrText.setText(String.valueOf(wcr));
            TextView bcrText = layout.findViewById(R.id.cannotStartGame_bcr);
            bcrText.setText(String.valueOf(bcr));
            TextView wcpText = layout.findViewById(R.id.cannotStartGame_wcp);
            wcpText.setText(String.valueOf(wcp));
            TextView bcpText = layout.findViewById(R.id.cannotStartGame_bcp);
            bcpText.setText(String.valueOf(bcp));
            ImageView bcCheck = layout.findViewById(R.id.cannotStartGame_checkBc);
            bcCheck.setImageResource(bcp >= bcr ? R.drawable.ic_done_black_48dp : R.drawable.ic_clear_black_48dp);
            ImageView wcCheck = layout.findViewById(R.id.cannotStartGame_checkWc);
            wcCheck.setImageResource(wcp >= wcr ? R.drawable.ic_done_black_48dp : R.drawable.ic_clear_black_48dp);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.cannotStartGame)
                    .setView(layout)
                    .setPositiveButton(android.R.string.ok, null);

            if (listener != null) listener.showDialog(builder);
        }
    }

    private void updateInstructions(String instructions) {
        this.instructions.setText(instructions);
    }

    public void setCards(@NonNull GameCards cards) {
        blackCard.setCard(cards.blackCard);
        playersCardsAdapter.setAssociatedBlackCard(cards.blackCard);
        updatePlayersCards(cards.whiteCards);
    }

    private void newBlackCard(@Nullable Card card) {
        blackCard.setCard(card);
        playersCardsAdapter.setAssociatedBlackCard(card);
    }

    private void handleMyStatus(GameInfo.PlayerStatus status) {
        lastMineStatus = status;
        switch (status) {
            case HOST:
                updateInstructions("You're the game host. Start the game when you're ready.");
                break;
            case IDLE:
                if (!lastPlaying) {
                    updateInstructions("Waiting for other players...");
                    lastPlaying = false;
                }

                if (whiteCards.getAdapter() != playersCardsAdapter)
                    whiteCards.swapAdapter(playersCardsAdapter, true);
                break;
            case JUDGE:
                updateInstructions("You're the Card Czar!");
                if (whiteCards.getAdapter() != playersCardsAdapter)
                    whiteCards.swapAdapter(playersCardsAdapter, true);
                break;
            case JUDGING:
                updateInstructions("Select the card(s) that will win this round.");
                if (whiteCards.getAdapter() != playersCardsAdapter)
                    whiteCards.swapAdapter(playersCardsAdapter, true);
                break;
            case PLAYING:
                if (blackCard.getCard() == null) break;
                updateInstructions("Select " + blackCard.getCard().getNumPick() + " card(s) to play. Your hand:");
                if (whiteCards.getAdapter() != handAdapter)
                    whiteCards.swapAdapter(handAdapter, true);
                break;
            case WINNER:
                updateInstructions("You won!");
                break;
            case SPECTATOR:
                updateInstructions("You're a spectator.");
                break;
        }
    }

    private void playerInfoChanged(GameInfo.Player player) {
        playersAdapter.notifyItemChanged(player);
        gameInfo.notifyPlayerChanged(player);
        if (Objects.equals(player.name, me.nickname)) {
            handleMyStatus(player.status);
        } else {
            switch (player.status) {
                case IDLE:
                case HOST:
                case PLAYING:
                case JUDGE:
                case WINNER:
                case SPECTATOR:
                    break;
                case JUDGING:
                    updateInstructions(player.name + " is judging...");
                    break;
            }
        }
    }

    private void updateBlankCardsNumber() {
        int numBlanks = 0;
        for (GameInfo.Player player : gameInfo.players)
            if (player.status == GameInfo.PlayerStatus.IDLE)
                numBlanks++;

        int missing = numBlanks - playersCardsAdapter.getItemCount();
        if (missing > 0)
            for (int i = 0; i <= missing; i++)
                playersCardsAdapter.addBlankCard();
    }

    private void handlePollMessage(PollMessage message) throws JSONException {
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
                updatePlayersCards(new ArrayList<List<Card>>());
                newBlackCard(null);
                if (listener != null)
                    listener.showToast(new Toaster.Message(R.string.judgeLeft, false));
                return;
            case GAME_OPTIONS_CHANGED:
                gameInfo = new GameInfo(new Game(message.obj.getJSONObject("gi")), gameInfo.players);
                break;
            case GAME_PLAYER_INFO_CHANGE:
                playerInfoChanged(new GameInfo.Player(message.obj.getJSONObject("pi")));
                updateBlankCardsNumber();
                break;
            case GAME_PLAYER_KICKED_IDLE:
            case GAME_PLAYER_JOIN:
            case GAME_PLAYER_LEAVE:
                refreshPlayersList();
                break;
            case GAME_PLAYER_SKIPPED:
                if (listener != null) listener.notifyPlayerSkipped(message.obj.getString("n"));
                break;
            case GAME_JUDGE_SKIPPED:
                if (listener != null) listener.notifyJudgeSkipped(message.obj.optString("n", null));
                break;
            case GAME_ROUND_COMPLETE:
                handleWinner(message.obj.getString("rw"), message.obj.getInt("WC") /* intermission: message.obj.getInt("i") */);
                break;
            case GAME_STATE_CHANGE:
                handleGameStateChange(Game.Status.parse(message.obj.getString("gs")), message);
                break;
            case HAND_DEAL:
                handleMyStatus(GameInfo.PlayerStatus.PLAYING);
                handleHandDeal(Card.toCardsList(message.obj.getJSONArray("h")));
                break;
            case HURRY_UP:
                if (listener != null) listener.showToast(Utils.Messages.HURRY_UP);
                break;
            case KICKED_FROM_GAME_IDLE:
                if (listener != null) listener.kicked();
                break;
        }
    }

    private void handleHandDeal(List<Card> cards) {
        if (cards.size() == 10) {
            List<List<Card>> cardLists = new ArrayList<>();
            for (Card card : cards) cardLists.add(Collections.singletonList(card));
            updateHand(cardLists);
        } else {
            addToHand(cards);
        }
    }

    private void updateHand(List<List<Card>> cardLists) {
        handAdapter.notifyDataSetChanged(cardLists);
    }

    private void removeFromHand(Card card) {
        handAdapter.notifyItemRemoved(card);
    }

    private void addToHand(List<Card> cards) {
        List<List<Card>> cardLists = new ArrayList<>();
        for (Card card : cards) cardLists.add(Collections.singletonList(card));
        handAdapter.notifyItemInserted(cardLists);
    }

    private void refreshPlayersList() {
        pyx.getGameInfo(gameInfo.game.gid, new PYX.IResult<GameInfo>() {
            @Override
            public void onDone(PYX pyx, GameInfo result) {
                GameManager.this.gameInfo = result;
                playersAdapter.notifyDataSetChanged(result.players);
                for (GameInfo.Player player : result.players)
                    playerInfoChanged(player);
            }

            @Override
            public void onException(Exception ex) {
                GameManager.this.onException(ex);
            }
        });
    }

    private void handleWinner(String winner, int winnerCard) {
        if (listener != null) listener.notifyWinner(winner);
        playersCardsAdapter.notifyWinningCard(winnerCard);
    }

    private void updatePlayersCards(List<List<Card>> whiteCards) {
        playersCardsAdapter.notifyDataSetChanged(whiteCards);
    }

    private void handleGameStateChange(Game.Status newStatus, PollMessage message) throws JSONException {
        gameInfo.game.setStatus(newStatus);
        switch (newStatus) {
            case ROUND_OVER:
            case DEALING:
                // Never called
                break;
            case JUDGING:
                updatePlayersCards(GameCards.toWhiteCardsList(message.obj.getJSONArray("wc")));
                isLobby(false);
                break;
            case LOBBY:
                newBlackCard(null);
                updatePlayersCards(new ArrayList<List<Card>>());
                handleHandDeal(new ArrayList<Card>());
                handleMyStatus(GameInfo.PlayerStatus.IDLE);
                isLobby(true);
                break;
            case PLAYING:
                updatePlayersCards(new ArrayList<List<Card>>());
                newBlackCard(new Card(message.obj.getJSONObject("bc")));
                refreshPlayersList();
                isLobby(false);
                break;
        }
    }

    @Override
    public void onDone(PYX pyx, List<PollMessage> result) {
        for (final PollMessage message : result) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        handlePollMessage(message);
                    } catch (JSONException ex) {
                        onException(ex);
                    }
                }
            });
        }
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(context, ex);
    }

    @Nullable
    @Override
    public RecyclerView getCardsRecyclerView() {
        return whiteCards;
    }

    private void setAmLastPlaying() {
        for (GameInfo.Player player : gameInfo.players) {
            if (!Objects.equals(player.name, me.nickname) && player.status == GameInfo.PlayerStatus.PLAYING) {
                lastPlaying = false;
                return;
            }
        }

        lastPlaying = true;
    }

    private void playCard(final Card card, int gid, int cid, @Nullable final String customText) {
        pyx.playCard(gid, cid, customText, new PYX.ISuccess() {
            @Override
            public void onDone(PYX pyx) {
                removeFromHand(card);
                updateBlankCardsNumber();
                setAmLastPlaying();

                HitBuilders.EventBuilder event = new HitBuilders.EventBuilder()
                        .setCategory(ThisApplication.CATEGORY_USER_INPUT);

                if (customText != null) event.setAction(ThisApplication.ACTION_PLAY_CUSTOM_CARD);
                else event.setAction(ThisApplication.ACTION_PLAY_CARD);

                ThisApplication.sendAnalytics(context, event.build());
            }

            @Override
            public void onException(Exception ex) {
                GameManager.this.onException(ex);
            }
        });
    }

    @Override
    public void onCardSelected(BaseCard baseCard) {
        final Card card = (Card) baseCard;
        if (lastMineStatus == GameInfo.PlayerStatus.PLAYING) {
            if (card.isWriteIn()) {
                final EditText customText = new EditText(context);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.setBlankCardText)
                        .setView(customText)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                playCard(card, gameInfo.game.gid, card.id, customText.getText().toString());
                            }
                        });

                CommonUtils.showDialog(context, builder);
            } else {
                playCard(card, gameInfo.game.gid, card.id, null);
            }
        } else if (lastMineStatus == GameInfo.PlayerStatus.JUDGING || lastMineStatus == GameInfo.PlayerStatus.JUDGE) {
            pyx.judgeCard(gameInfo.game.gid, card.id, new PYX.ISuccess() {
                @Override
                public void onDone(PYX pyx) {
                    handleMyStatus(GameInfo.PlayerStatus.PLAYING);

                    ThisApplication.sendAnalytics(context, new HitBuilders.EventBuilder()
                            .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                            .setAction(ThisApplication.ACTION_JUDGE_CARD)
                            .build());
                }

                @Override
                public void onException(Exception ex) {
                    GameManager.this.onException(ex);
                }
            });
        }
    }

    @Override
    public void onDeleteCard(StarredCardsManager.StarredCard card) {
        // Never called
    }

    public interface IManager {
        void notifyWinner(String nickname);

        void notifyPlayerSkipped(String nickname);

        void notifyJudgeSkipped(@Nullable String nickname);

        void cannotStartGame(Exception ex);

        void kicked();

        void showToast(Toaster.Message message);

        void showDialog(AlertDialog.Builder builder);
    }
}
