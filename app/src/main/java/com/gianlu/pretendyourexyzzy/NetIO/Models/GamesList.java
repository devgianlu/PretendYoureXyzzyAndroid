package com.gianlu.pretendyourexyzzy.NetIO.Models;

import java.util.ArrayList;

public class GamesList extends ArrayList<Game> {
    public final int maxGames;

    public GamesList(int maxGames) {
        this.maxGames = maxGames;
    }
}
