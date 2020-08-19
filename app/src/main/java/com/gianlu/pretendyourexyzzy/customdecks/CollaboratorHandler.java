package com.gianlu.pretendyourexyzzy.customdecks;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.AbsCardsFragment.CardActionCompleteCallback;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomCard;

import java.util.List;

public final class CollaboratorHandler implements AbsCardsFragment.CardActionsHandler { // TODO
    private final String shareCode;

    public CollaboratorHandler(@NonNull String shareCode) {
        this.shareCode = shareCode;
    }

    @Override
    public void removeCard(@NonNull BaseCard oldCard, @NonNull CardActionCompleteCallback<Void> callback) {

    }

    @Override
    public void updateCard(@NonNull BaseCard oldCard, @NonNull String[] text, @NonNull CardActionCompleteCallback<CustomCard> callback) {

    }

    @Override
    public void addCard(boolean black, @NonNull String[] text, @NonNull CardActionCompleteCallback<CustomCard> callback) {

    }

    @Override
    public void addCards(boolean[] blacks, @NonNull String[][] texts, @NonNull CardActionCompleteCallback<List<CustomCard>> callback) {

    }
}
