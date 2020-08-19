package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.AbsCardsFragment.CardActionCompleteCallback;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomCard;

import java.util.List;

public final class CustomDecksHandler implements AbsCardsFragment.CardActionsHandler {
    public final int id;
    private final CustomDecksDatabase db;

    public CustomDecksHandler(@NonNull Context context, int id) {
        this.db = CustomDecksDatabase.get(context);
        this.id = id;
    }

    @Override
    public void removeCard(@NonNull BaseCard oldCard, @NonNull CardActionCompleteCallback<Void> callback) {
        db.removeCard(id, ((CustomCard) oldCard).id);
        callback.onComplete(null);
    }

    @Override
    public void updateCard(@NonNull BaseCard oldCard, @NonNull String[] text, @NonNull CardActionCompleteCallback<CustomCard> callback) {
        CustomCard card = db.updateCard(id, (CustomCard) oldCard, text);
        callback.onComplete(card);
    }

    @Override
    public void addCard(boolean black, @NonNull String[] text, @NonNull CardActionCompleteCallback<CustomCard> callback) {
        CustomCard card = db.putCard(id, black, text);
        callback.onComplete(card);
    }

    @Override
    public void addCards(boolean[] blacks, @NonNull String[][] texts, @NonNull CardActionCompleteCallback<List<CustomCard>> callback) {
        List<CustomCard> cards = db.putCards(id, blacks, texts);
        callback.onComplete(cards);
    }
}
