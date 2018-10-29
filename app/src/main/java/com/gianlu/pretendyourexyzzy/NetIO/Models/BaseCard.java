package com.gianlu.pretendyourexyzzy.NetIO.Models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class BaseCard implements Serializable {
    private transient String imageUrl = null;

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
    public final String getImageUrl() {
        if (black()) return null;

        if (imageUrl == null) {
            String text = text();
            if (text.startsWith("[img]") && text.endsWith("[/img]"))
                imageUrl = text.substring(5, text.length() - 6);
        }

        return imageUrl;
    }

    @Nullable
    public abstract JSONObject toJson() throws JSONException;

    @Override
    public final int hashCode() {
        return id() * 31 + text().hashCode();
    }
}
