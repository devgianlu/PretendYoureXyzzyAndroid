package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.DiffUtil;

public class SensitiveGameData {
    final List<GameInfo.Player> players = new ArrayList<>();
    private final int gid;
    private final String me;
    private final Listener listener;
    private final Set<String> spectators = new HashSet<>();
    AdapterInterface playersInterface;
    private String host;
    private Game.Status status;
    private Game.Options options;
    private String judge;

    SensitiveGameData(int gid, RegisteredPyx pyx, Listener listener) {
        this.gid = gid;
        this.me = pyx.user().nickname;
        this.listener = listener;
    }

    boolean amHost() {
        return host.equals(me);
    }

    void update(@NonNull GameInfo info, @Nullable GameLayout layout) {
        List<GameInfo.Player> oldPlayers = new ArrayList<>(players);
        players.clear();
        players.addAll(info.players);
        if (layout != null) layout.setup(this);

        for (GameInfo.Player player : players)
            playerChange(player, oldPlayers);

        if (playersInterface != null)
            playersInterface.dispatchUpdate(DiffUtil.calculateDiff(new PlayersDiff(oldPlayers, players), false));

        update(info.game);
    }

    void update(@NonNull Game game) {
        spectators.clear();
        spectators.addAll(game.spectators);

        host = game.host;
        status = game.status;
        options = game.options;
    }

    void spectatorJoin(String name) {
        synchronized (spectators) {
            spectators.add(name);
        }
    }

    void spectatorLeave(String name) {
        synchronized (spectators) {
            spectators.remove(name);
        }
    }

    void update(Game.Status status) {
        this.status = status;
    }

    boolean amJudge() {
        return me.equals(judge);
    }

    void playerChange(@NonNull GameInfo.Player player) {
        playerChange(player, players);
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name.equals(player.name)) {
                players.set(i, player);
                if (playersInterface != null) playersInterface.notifyItemChanged(i);
            }
        }
    }

    private void playerChange(@NonNull GameInfo.Player player, List<GameInfo.Player> oldPlayers) {
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
        if (player.name.equals(me))
            listener.ourPlayerChanged(player);

        if (player.status == GameInfo.PlayerStatus.JUDGE || player.status == GameInfo.PlayerStatus.JUDGING)
            judge = player.name;

        if (oldStatus == GameInfo.PlayerStatus.PLAYING && player.status == GameInfo.PlayerStatus.IDLE
                && !player.name.equals(me) && status == Game.Status.PLAYING)
            listener.anotherPlayerPlayed();
    }

    public interface Listener {
        void ourPlayerChanged(@NonNull GameInfo.Player player);

        void anotherPlayerPlayed();
    }

    @UiThread
    public interface AdapterInterface {
        void notifyItemInserted(int pos);

        void notifyItemRemoved(int pos);

        void dispatchUpdate(@NonNull DiffUtil.DiffResult result);

        void notifyItemChanged(int pos);
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
