package com.gianlu.pretendyourexyzzy.customdecks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.adapters.Filterable;

public class BasicCustomDeck implements Filterable<Void> {
    public final String name;
    public final String watermark;
    public final String owner;
    public final long lastUsed;
    private final int count;

    protected BasicCustomDeck(@NonNull String name, @NonNull String watermark, @Nullable String owner, long lastUsed) {
        this(name, watermark, owner, lastUsed, -1);
    }

    protected BasicCustomDeck(@NonNull String name, @NonNull String watermark, @Nullable String owner, long lastUsed, int count) {
        this.name = name;
        this.watermark = watermark;
        this.owner = owner;
        this.lastUsed = lastUsed;
        this.count = count;
    }

    public int cardsCount() {
        return count;
    }

    public int whiteCardsCount() {
        return -1;
    }

    public int blackCardsCount() {
        return -1;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + watermark.hashCode();
        return result;
    }

    @Override
    @Nullable
    public Void[] getMatchingFilters() {
        return null;
    }
}
