package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.commonutils.Logging;
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
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GameManager implements PYX.IResult<List<PollMessage>>, CardsAdapter.IAdapter {
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
    public GameInfo gameInfo;
    private GameInfo.PlayerStatus lastStatus;
    private boolean firstHandDeal = true;

    public GameManager(ViewGroup gameLayout, @NonNull GameInfo gameInfo, User me, IManager listener) {
        this.context = gameLayout.getContext();
        this.gameInfo = gameInfo;
        this.me = me;
        this.listener = listener;
        this.handler = new Handler(context.getMainLooper());
        this.pyx = PYX.get(context);

        blackCard = (PyxCard) gameLayout.findViewById(R.id.gameLayout_blackCard);
        RecyclerView playersList = (RecyclerView) gameLayout.findViewById(R.id.gameLayout_players);
        playersList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        playersAdapter = new PlayersAdapter(context, gameInfo.players);
        playersList.setAdapter(playersAdapter);

        instructions = (TextView) gameLayout.findViewById(R.id.gameLayout_instructions);
        whiteCards = (RecyclerView) gameLayout.findViewById(R.id.gameLayout_whiteCards);
        whiteCards.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        playersCardsAdapter = new CardsAdapter(context, this);
        handAdapter = new CardsAdapter(context, this);
        whiteCards.setAdapter(playersCardsAdapter);

        if (gameInfo.game.spectators.contains(me.nickname)) {
            updateInstructions("You're a spectator.");
        }

        GameInfo.Player alsoMe = Utils.find(gameInfo.players, me.nickname);
        if (alsoMe != null) handleMyStatus(alsoMe.status);
    }

    private void updateInstructions(String instructions) {
        this.instructions.setText(instructions);
    }

    public void setCards(@NonNull GameCards cards) {
        blackCard.setCard(cards.blackCard);
        playersCardsAdapter.setAssociatedBlackCard(cards.blackCard);
        updatePlayersCards(cards.whiteCards);
    }

    private void newBlackCard(Card card) {
        blackCard.setCard(card);
        playersCardsAdapter.setAssociatedBlackCard(card);
    }

    private void handleMyStatus(GameInfo.PlayerStatus status) {
        lastStatus = status;
        switch (status) {
            case HOST:
                updateInstructions("You're the game host. Start the game when you're ready.");
                break;
            case IDLE:
                updateInstructions("Waiting for other players...");
                whiteCards.swapAdapter(playersCardsAdapter, true);
                break;
            case JUDGE:
                updateInstructions("You're the Card Czar!");
                whiteCards.swapAdapter(playersCardsAdapter, true);
                break;
            case JUDGING:
                updateInstructions("Select the card(s) that will win this round.");
                whiteCards.swapAdapter(playersCardsAdapter, true);
                break;
            case PLAYING:
                updateInstructions("Select the card(s) to play. Your hand:");
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
        for (GameInfo.Player player : playersAdapter.getPlayers())
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
            case GAME_LIST_REFRESH:
            case NOOP:
            case CHAT:
            case GAME_BLACK_RESHUFFLE:
            case GAME_JUDGE_LEFT:
            case GAME_SPECTATOR_JOIN:
            case GAME_WHITE_RESHUFFLE:
            case GAME_SPECTATOR_LEAVE:
                // Not interested in these
                return;
            case GAME_OPTIONS_CHANGED:
                gameInfo = new GameInfo(message.obj.getJSONObject("gi"));
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
                handleWinner(message.obj.getString("rw"), message.obj.getInt("WC"), message.obj.getInt("i"));
                break;
            case GAME_STATE_CHANGE:
                handleGameStateChange(Game.Status.parse(message.obj.getString("gs")), message);
                break;
            case HAND_DEAL:
                handleMyStatus(GameInfo.PlayerStatus.PLAYING);
                handleHandDeal(Card.toCardsList(message.obj.getJSONArray("h")));
                break;
            case HURRY_UP:
                if (listener != null) listener.hurryUp();
                break;
        }
    }

    private void handleHandDeal(List<Card> cards) {
        if (firstHandDeal) {
            List<List<Card>> cardLists = new ArrayList<>();
            for (Card card : cards) cardLists.add(Collections.singletonList(card));
            updateHand(cardLists);
            firstHandDeal = false;
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
            }

            @Override
            public void onException(Exception ex) {
                GameManager.this.onException(ex);
            }
        });
    }

    private void handleWinner(String winner, int winnerCard, int intermission) {
        if (listener != null) listener.notifyWinner(winner);
        playersCardsAdapter.notifyWinningCard(winnerCard);

        /*
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateWhiteCards(new ArrayList<List<Card>>());
                    }
                });
            }
        }, intermission);
        */
    }

    private void updatePlayersCards(List<List<Card>> whiteCards) {
        playersCardsAdapter.notifyDataSetChanged(whiteCards);
    }

    private void handleGameStateChange(Game.Status newStatus, PollMessage message) throws JSONException {
        switch (newStatus) {
            case DEALING:
                break;
            case JUDGING:
                updatePlayersCards(GameCards.toWhiteCardsList(message.obj.getJSONArray("wc")));
                break;
            case LOBBY:
                break;
            case PLAYING:
                updatePlayersCards(new ArrayList<List<Card>>());
                newBlackCard(new Card(message.obj.getJSONObject("bc")));
                break;
            case ROUND_OVER:
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

    @Override
    public void onCardSelected(BaseCard baseCard) {
        final Card card = (Card) baseCard;

        if (lastStatus == GameInfo.PlayerStatus.PLAYING) {
            pyx.playCard(gameInfo.game.gid, card.id, null, new PYX.ISuccess() {
                @Override
                public void onDone(PYX pyx) {
                    removeFromHand(card);
                    handleMyStatus(GameInfo.PlayerStatus.IDLE);
                    updateBlankCardsNumber();
                }

                @Override
                public void onException(Exception ex) {
                    GameManager.this.onException(ex);
                }
            });
        } else if (lastStatus == GameInfo.PlayerStatus.JUDGING) {
            pyx.judgeCard(gameInfo.game.gid, card.id, new PYX.ISuccess() {
                @Override
                public void onDone(PYX pyx) {
                    handleMyStatus(GameInfo.PlayerStatus.PLAYING);
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

        void hurryUp();
    }
}
