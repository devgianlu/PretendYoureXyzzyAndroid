package com.gianlu.pretendyourexyzzy.api.models.cards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UnknownCard extends BaseCard {

    public UnknownCard() {
    }

    @NonNull
    @Override
    public String text() {
        return "";
    }

    @Nullable
    @Override
    public String watermark() {
        return null;
    }

    @Override
    public int numPick() {
        return -1;
    }

    @Override
    public int numDraw() {
        return -1;
    }

    @Override
    public boolean black() {
        return false;
    }
}
