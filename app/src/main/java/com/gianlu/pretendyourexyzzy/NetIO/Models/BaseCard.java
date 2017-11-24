package com.gianlu.pretendyourexyzzy.NetIO.Models;

public interface BaseCard {
    String getText();

    String getWatermark();

    int getNumPick();

    int getNumDraw();

    int getId();

    boolean equals(Object o);

    boolean isUnknown();

    boolean isBlack();
}
