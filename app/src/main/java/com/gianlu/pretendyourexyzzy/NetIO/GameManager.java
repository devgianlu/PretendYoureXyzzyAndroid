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
import com.gianlu.pretendyourexyzzy.Adapters.PyxCard;
import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameCards;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class GameManager implements PYX.IResult<List<PollMessage>>, CardsAdapter.IAdapter {
    private final PyxCard blackCard;
    private final RecyclerView playersList;
    private final TextView instructions;
    private final RecyclerView whiteCards;
    private final Context context;
    private final PlayersAdapter playersAdapter;
    private final CardsAdapter whiteCardsAdapter;
    private final Handler handler;
    private final IManager listener;
    private final PYX pyx;
    public GameInfo gameInfo;

    public GameManager(ViewGroup gameLayout, @NonNull GameInfo gameInfo, User me, IManager listener) {
        this.context = gameLayout.getContext();
        this.gameInfo = gameInfo;
        this.listener = listener;
        this.handler = new Handler(context.getMainLooper());
        this.pyx = PYX.get(context);

        blackCard = (PyxCard) gameLayout.findViewById(R.id.gameLayout_blackCard);
        playersList = (RecyclerView) gameLayout.findViewById(R.id.gameLayout_players);
        playersList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        playersList.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        playersAdapter = new PlayersAdapter(context, gameInfo.players);
        playersList.setAdapter(playersAdapter);

        instructions = (TextView) gameLayout.findViewById(R.id.gameLayout_instructions);
        whiteCards = (RecyclerView) gameLayout.findViewById(R.id.gameLayout_whiteCards);
        whiteCards.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        whiteCardsAdapter = new CardsAdapter(context, this);
        whiteCards.setAdapter(whiteCardsAdapter);

        if (gameInfo.game.spectators.contains(me.nickname)) {
            updateInstructions("You're a spectator.");
        }
    }

    private void updateInstructions(String instructions) {
        this.instructions.setText(instructions);
    }

    public void setCards(@NonNull GameCards cards) {
        blackCard.setCard(cards.blackCard);
        whiteCardsAdapter.setAssociatedBlackCard(cards.blackCard);
        updateWhiteCards(cards.whiteCards);
    }

    private void newBlackCard(Card card) {
        blackCard.setCard(card);
        whiteCardsAdapter.setAssociatedBlackCard(card);
    }

    private void playerInfoChanged(GameInfo.Player player) {
        playersAdapter.notifyItemChanged(player);
    }

    private void updateBlankCardsNumber() {
        int numBlanks = 0;
        for (GameInfo.Player player : playersAdapter.getPlayers())
            if (player.status == GameInfo.PlayerStatus.IDLE)
                numBlanks++;

        if (whiteCardsAdapter.getItemCount() < numBlanks)
            whiteCardsAdapter.addBlankCard();
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
                if (listener != null) listener.notifyJudgeSkipped(message.obj.getString("n"));
                break;
            case GAME_ROUND_COMPLETE:
                handleWinner(message.obj.getString("rw"), message.obj.getInt("WC"), message.obj.getInt("i"));
                break;
            case GAME_STATE_CHANGE:
                handleGameStateChange(Game.Status.parse(message.obj.getString("gs")), message);
                break;
        }
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
        whiteCardsAdapter.notifyWinningCard(winnerCard);

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

    private void updateWhiteCards(List<List<Card>> whiteCards) {
        whiteCardsAdapter.notifyDataSetChanged(whiteCards);
    }

    private void handleGameStateChange(Game.Status newStatus, PollMessage message) throws JSONException {
        switch (newStatus) {
            case DEALING:
                break;
            case JUDGING:
                updateWhiteCards(GameCards.toWhiteCardsList(message.obj.getJSONArray("wc")));
                break;
            case LOBBY:
                break;
            case PLAYING:
                updateWhiteCards(new ArrayList<List<Card>>());
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

    public interface IManager {
        void notifyWinner(String nickname);

        void notifyPlayerSkipped(String nickname);

        void notifyJudgeSkipped(String nickname);
    }
}
