package com.gianlu.pretendyourexyzzy.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.DiffUtil;

import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GameCards;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SensitiveGameData {
    public final List<GameInfo.Player> players = new ArrayList<>();
    public final Set<String> spectators = new HashSet<>();
    public final String me;
    private final Listener listener;
    public volatile boolean hasPassword;
    public volatile AdapterInterface playersInterface;
    public volatile String host;
    public volatile Game.Status status;
    public volatile Game.Options options;
    public volatile String judge;
    public volatile String lastRoundPermalink;

    SensitiveGameData(@NonNull RegisteredPyx pyx, @NonNull Listener listener) {
        this.me = pyx.user().nickname;
        this.listener = listener;
    }

    boolean amHost() {
        return Objects.equals(host, me);
    }

    @UiThread
    void update(@NonNull GameInfo info) {
        update(info, null, null);
    }

    @UiThread
    void update(@NonNull GameInfo info, @Nullable GameCards cards, @Nullable GameUi ui) {
        update(info.game);

        List<GameInfo.Player> oldPlayers = new ArrayList<>(players);
        synchronized (players) {
            players.clear();
            players.addAll(info.players);
        }

        if (ui != null) {
            ui.setup(this);
            if (cards != null) {
                ui.setHand(cards.hand);
                ui.setBlackCard(cards.blackCard);
                ui.setTable(cards.whiteCards, cards.blackCard);
            }
        }

        synchronized (spectators) {
            for (String spectator : spectators) {
                if (spectator.equals(me))
                    listener.playerIsSpectator();
            }
        }

        synchronized (players) {
            for (GameInfo.Player player : players)
                playerChange(player, oldPlayers);
        }

        if (playersInterface != null)
            playersInterface.dispatchUpdate(DiffUtil.calculateDiff(new PlayersDiff(oldPlayers, players), false));
    }

    void update(@NonNull Game game) {
        synchronized (spectators) {
            spectators.clear();
            spectators.addAll(game.spectators);
        }

        host = game.host;
        hasPassword = game.hasPassword;
        status = game.status;
        options = game.options;
    }

    void spectatorJoin(@NonNull String name) {
        synchronized (spectators) {
            spectators.add(name);
        }
    }

    void spectatorLeave(@NonNull String name) {
        synchronized (spectators) {
            spectators.remove(name);
        }
    }

    void update(@NonNull Game.Status status) {
        this.status = status;
    }

    boolean amJudge() {
        return me.equals(judge);
    }

    @UiThread
    void playerChange(@NonNull GameInfo.Player player) {
        playerChange(player, players);
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(player.name)) {
                synchronized (players) {
                    players.set(i, player);
                }

                if (playersInterface != null) playersInterface.notifyItemChanged(i);
                return;
            }
        }
    }

    private void playerChange(@NonNull GameInfo.Player player, @NonNull List<GameInfo.Player> oldPlayers) {
        GameInfo.PlayerStatus oldStatus = null;
        for (GameInfo.Player oldPlayer : oldPlayers) {
            if (oldPlayer.name.equals(player.name)) {
                oldStatus = oldPlayer.status;
                break;
            }
        }

        playerChangeInternal(player, oldStatus);
    }

    private void playerChangeInternal(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus) {
        if (player.status == GameInfo.PlayerStatus.JUDGE || player.status == GameInfo.PlayerStatus.JUDGING)
            judge = player.name;

        if (player.status == GameInfo.PlayerStatus.HOST)
            host = player.name;

        if (player.name.equals(me)) listener.ourPlayerChanged(player, oldStatus);
        else listener.notOutPlayerChanged(player, oldStatus);

        listener.anyPlayerChanged(player, oldStatus);
    }

    boolean amSpectator() {
        synchronized (spectators) {
            return spectators.contains(me);
        }
    }

    @UiThread
    void resetToIdleAndHost() {
        synchronized (players) {
            for (GameInfo.Player player : players) {
                if (player.name.equals(host)) player.status = GameInfo.PlayerStatus.HOST;
                else player.status = GameInfo.PlayerStatus.IDLE;
            }
        }

        playersInterface.clearPool();
        playersInterface.notifyDataSetChanged();
    }

    boolean isPlayerStatus(@NonNull String me, @NonNull GameInfo.PlayerStatus status) {
        synchronized (players) {
            for (GameInfo.Player player : players) {
                if (Objects.equals(player.name, me))
                    return player.status == status;
            }

            return false;
        }
    }

    public interface Listener {
        void ourPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus);

        void anyPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus);

        void notOutPlayerChanged(@NonNull GameInfo.Player player, @Nullable GameInfo.PlayerStatus oldStatus);

        void playerIsSpectator();
    }

    @UiThread
    public interface AdapterInterface {
        void notifyDataSetChanged();

        void dispatchUpdate(@NonNull DiffUtil.DiffResult result);

        void notifyItemChanged(int pos);

        void clearPool();
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
    }
}
