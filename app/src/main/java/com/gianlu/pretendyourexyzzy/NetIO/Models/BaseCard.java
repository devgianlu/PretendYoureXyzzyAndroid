package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public interface BaseCard {
    @NonNull
    String text();

    @Nullable
    String watermark();

    int numPick();

    int numDraw();

    int id();

    boolean equals(Object o);

    boolean unknown();

    boolean black();

    @Nullable
    JSONObject toJson() throws JSONException;
}
