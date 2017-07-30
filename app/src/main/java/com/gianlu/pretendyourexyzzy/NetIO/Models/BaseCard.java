package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONException;
import org.json.JSONObject;

public interface BaseCard {
    String getText();

    String getWatermark();

    int getNumPick();

    int getNumDraw();

    boolean isWriteIn();

    int getId();

    boolean isWinning();

    void setWinning(boolean winning);

    JSONObject toJSON() throws JSONException;

    boolean equals(Object o);
}
