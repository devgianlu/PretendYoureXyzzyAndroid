package com.gianlu.pretendyourexyzzy.api.models;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;

public class GamesList extends ArrayList<Game> {
    public final int maxGames;

    public GamesList(int maxGames) {
        this.maxGames = maxGames;
    }

    public static class DiffCallback extends DiffUtil.Callback {
        private final GamesList newList;
        private final List<Game> oldList;

        public DiffCallback(GamesList newList, List<Game> oldList) {
            this.newList = newList;
            this.oldList = oldList;
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
            return oldList.get(oldPos).gid == newList.get(newPos).gid;
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).equals(newList.get(newPos));
        }
    }
}
