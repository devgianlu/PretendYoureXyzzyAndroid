package com.gianlu.pretendyourexyzzy.customdecks;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomCard;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

public final class CustomDecksHandler implements CardActionsHandler {
    public final int id;
    private final CustomDecksDatabase db;

    public CustomDecksHandler(@NonNull CustomDecksDatabase db, int id) {
        this.db = db;
        this.id = id;
    }

    @NonNull
    @Override
    public Task<Void> removeCard(@NonNull BaseCard oldCard) {
        db.removeCard(id, ((CustomCard) oldCard).id);
        return Tasks.forResult(null);
    }

    @NonNull
    @Override
    public Task<BaseCard> updateCard(@NonNull BaseCard oldCard, @NonNull String[] text) {
        CustomCard card = db.updateCard(id, (CustomCard) oldCard, text);
        return Tasks.forResult(card);
    }

    @NonNull
    @Override
    public Task<BaseCard> addCard(boolean black, @NonNull String[] text) {
        CustomCard card = db.putCard(id, black, text);
        return Tasks.forResult(card);
    }

    @NonNull
    @Override
    public Task<List<? extends BaseCard>> addCards(boolean[] blacks, @NonNull String[][] texts) {
        List<CustomCard> cards = db.putCards(id, blacks, texts);
        return Tasks.forResult(cards);
    }
}
