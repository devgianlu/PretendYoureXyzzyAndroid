package com.gianlu.pretendyourexyzzy.customdecks;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CardActionsHandler {
    @NotNull
    Task<Void> removeCard(@NonNull BaseCard oldCard);

    @NotNull
    Task<BaseCard> updateCard(@NonNull BaseCard oldCard, @NonNull String[] text);

    @NotNull
    Task<BaseCard> addCard(boolean black, @NonNull String[] text);

    @NotNull
    Task<List<? extends BaseCard>> addCards(boolean[] blacks, @NonNull String[][] texts);
}
