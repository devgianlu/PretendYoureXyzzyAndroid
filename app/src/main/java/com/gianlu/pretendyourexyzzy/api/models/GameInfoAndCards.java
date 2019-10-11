package com.gianlu.pretendyourexyzzy.api.models;

public class GameInfoAndCards {
    public final GameInfo info;
    public final GameCards cards;

    public GameInfoAndCards(GameInfo info, GameCards cards) {
        this.info = info;
        this.cards = cards;
    }
}
