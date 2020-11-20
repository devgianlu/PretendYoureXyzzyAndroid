package com.gianlu.pretendyourexyzzy.game;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GameCards;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class to keep the game data organized. This will mostly take care of updating stuff and notifying listeners and adapters.
 */
@UiThread
public class GameData {
    public final PlayersList players = new PlayersList();
    public final Set<String> spectators = new HashSet<>();
    public final String me;
    private final Listener listener;
    public volatile boolean hasPassword;
    public volatile RecyclerView.Adapter<?> playersAdapter;
    public volatile String host;
    public volatile Game.Status status;
    public volatile Game.Options options;
    public volatile String judge;
    public volatile String lastRoundPermalink;

    GameData(@NonNull String me, @NonNull Listener listener) {
        this.me = me;
        this.listener = listener;
    }

    /**
     * Updates everything, setup the UI if provided, notify players listeners and adapter
     *
     * @param info  The new {@link GameInfo}
     * @param cards The new {@link GameCards}
     * @param ui    The {@link GameUi}
     */
    @UiThread
    void update(@NonNull GameInfo info, @Nullable GameCards cards, @Nullable GameUi ui) {
        updateGame(info.game);

        PlayersList oldPlayers = players.copy();
        players.update(info.players);

        if (ui != null) {
            ui.setup(this);
            if (cards != null) {
                ui.setHand(cards.hand);
                ui.setBlackCard(cards.blackCard);
                ui.setTable(cards.whiteCards, cards.blackCard);
            }

            if (info.game.status == Game.Status.LOBBY) ui.showLobby();
            else ui.hideLobby();
        }

        for (String spectator : spectators) {
            if (spectator.equals(me)) listener.playerIsSpectator();
        }

        for (GameInfo.Player player : players)
            playerChangeInternal(player, oldPlayers.findByName(player.name));

        if (playersAdapter != null)
            DiffUtil.calculateDiff(new PlayersDiff(oldPlayers, players)).dispatchUpdatesTo(playersAdapter);
    }

    /**
     * Updates only "basic" data without notifying anything.
     *
     * @param game The new {@link Game}
     */
    void updateGame(@NonNull Game game) {
        spectators.clear();
        spectators.addAll(game.spectators);

        host = game.host;
        hasPassword = game.hasPassword;
        status = game.status;
        options = game.options;
    }

    /**
     * A spectator joined the game.
     *
     * @param name The spectator name
     */
    void spectatorJoin(@NonNull String name) {
        spectators.add(name);
    }

    /**
     * A spectator left the game.
     *
     * @param name The spectator name
     */
    void spectatorLeave(@NonNull String name) {
        spectators.remove(name);
    }

    /**
     * Update the game status
     *
     * @param status The new {@link Game.Status}
     */
    void updateStatus(@NonNull Game.Status status) {
        this.status = status;
    }

    /**
     * Updates internal variables if necessary, notifies listeners and notifies the adapter.
     *
     * @param player The new player
     */
    void playerChange(@NonNull GameInfo.Player player) {
        playerChangeInternal(player, players.findByName(player.name) /* This is still the old one */);

        Pair<Integer, Integer> change = players.update(player);
        if (playersAdapter != null && change != null) {
            if (!change.first.equals(change.second))
                playersAdapter.notifyItemMoved(change.first, change.second);

            playersAdapter.notifyItemChanged(change.second, player);
        }
    }

    /**
     * Updates internal variables if necessary and notifies listeners.
     *
     * @param player    The new player
     * @param oldPlayer The old player
     */
    private void playerChangeInternal(@NonNull GameInfo.Player player, @Nullable GameInfo.Player oldPlayer) {
        GameInfo.PlayerStatus oldStatus = oldPlayer == null ? null : oldPlayer.status;

        if (player.status == GameInfo.PlayerStatus.JUDGE || player.status == GameInfo.PlayerStatus.JUDGING)
            judge = player.name;

        if (player.status == GameInfo.PlayerStatus.HOST)
            host = player.name;

        if (player.name.equals(me)) listener.ourPlayerChanged(player, oldStatus);
        else listener.notOutPlayerChanged(player, oldStatus);

        listener.anyPlayerChanged(player, oldStatus);
    }

    /**
     * The game is in lobby state, reset everything.
     */
    void resetToIdleAndHost() {
        for (GameInfo.Player player : players) {
            if (player.name.equals(host)) player.status = GameInfo.PlayerStatus.HOST;
            else player.status = GameInfo.PlayerStatus.IDLE;
        }

        playersAdapter.notifyDataSetChanged();
    }

    //region Getters

    /**
     * @return Whether "our player" is the judge
     */
    boolean amJudge() {
        return Objects.equals(judge, me);
    }

    /**
     * @return Whether "our player" is the host
     */
    boolean amHost() {
        return Objects.equals(host, me);
    }

    /**
     * @return Whether "our player" is a spectator
     */
    boolean amSpectator() {
        return spectators.contains(me);
    }

    /**
     * Check if the given player exists and is in the given status.
     *
     * @param name   The player username
     * @param status The player status to check
     * @return Whether the player is in the given status
     */
    boolean isPlayerStatus(@NonNull String name, @NonNull GameInfo.PlayerStatus status) {
        GameInfo.Player player = players.findByName(name);
        return player != null && player.status == status;
    }
    //endregion

    public interface Listener {
        void ourPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus);

        void anyPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus);

        void notOutPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus);

        void playerIsSpectator();
    }

    private static class PlayersList extends ArrayList<GameInfo.Player> {
        PlayersList() {
            super(10);
        }

        private PlayersList(@NonNull PlayersList list) {
            super(list);
        }

        private void resort() {
            Collections.sort(this, (o1, o2) -> {
                if (o1.status == GameInfo.PlayerStatus.HOST)
                    return -1;
                else if (o2.status == GameInfo.PlayerStatus.HOST)
                    return 1;

                // TODO: Finish sorting

                return o1.name.compareToIgnoreCase(o2.name);
            });
        }

        @Nullable
        GameInfo.Player findByName(@NonNull String name) {
            for (GameInfo.Player player : this)
                if (player.name.equals(name))
                    return player;

            return null;
        }

        public int indexOfByName(@NonNull String name) {
            for (int i = 0; i < size(); i++)
                if (get(i).name.equals(name))
                    return i;

            return -1;
        }

        @Nullable
        Pair<Integer, Integer> update(@NonNull GameInfo.Player player) {
            int from = indexOfByName(player.name);
            if (from == -1)
                return null;

            set(from, player);
            int to = indexOfByName(player.name);
            return new Pair<>(from, to);
        }

        void update(@NonNull List<GameInfo.Player> players) {
            clear();
            addAll(players);
            resort();
        }

        @NonNull
        PlayersList copy() {
            return new PlayersList(this);
        }
    }

    private static class PlayersDiff extends DiffUtil.Callback {
        private final List<GameInfo.Player> oldList;
        private final List<GameInfo.Player> newList;

        PlayersDiff(List<GameInfo.Player> oldList, List<GameInfo.Player> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).name.equals(newList.get(newPos).name);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            GameInfo.Player oldPlayer = oldList.get(oldPos);
            GameInfo.Player newPlayer = newList.get(newPos);
            return oldPlayer.score == newPlayer.score && oldPlayer.status == newPlayer.status;
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldPos, int newPos) {
            return newList.get(newPos);
        }
    }
}
