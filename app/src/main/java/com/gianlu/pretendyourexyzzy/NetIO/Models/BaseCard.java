package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseCard {
    @NonNull
    public abstract String text();

    @Nullable
    public abstract String watermark();

    public abstract int numPick();

    public abstract int numDraw();

    public abstract int id();

    public abstract boolean equals(Object o);

    public abstract boolean unknown();

    public abstract boolean black();

    public abstract boolean writeIn();

    @Nullable
    public abstract JSONObject toJson() throws JSONException;

    @Override
    public int hashCode() {
        return id() * 31 + text().hashCode();
    }
}
