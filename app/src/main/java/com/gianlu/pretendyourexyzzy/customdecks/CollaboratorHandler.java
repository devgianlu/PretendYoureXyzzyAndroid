package com.gianlu.pretendyourexyzzy.customdecks;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CollaboratorPatchOp;
import xyz.gianlu.pyxoverloaded.model.Card;

public final class CollaboratorHandler implements CardActionsHandler {
    private static final String TAG = CollaboratorHandler.class.getSimpleName();
    private final String shareCode;
    private final String watermark;

    public CollaboratorHandler(@NonNull String shareCode, @NonNull String watermark) {
        this.shareCode = shareCode;
        this.watermark = watermark;
    }

    @Nullable
    private static Card getOverloadedCard(@NonNull BaseCard card) {
        if (card instanceof ContentCard) {
            if (((ContentCard) card).original instanceof Card) {
                Card overloadedCard = (Card) ((ContentCard) card).original;
                if (overloadedCard.remoteId == null) return null;
                else return overloadedCard;
            }
        }

        return null;
    }

    @NonNull
    @Override
    public Task<Void> removeCard(@NonNull BaseCard oldCard) {
        Card card = getOverloadedCard(oldCard);
        if (card == null) return Tasks.forCanceled();

        return OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.REM_CARD, null, card.remoteId, null)
                .continueWith((Continuation<OverloadedSyncApi.CollaboratorPatchResponse, Void>) task -> {
                    task.getResult();
                    return null;
                })
                .addOnFailureListener(ex -> Log.e(TAG, "Failed removing card.", ex));
    }

    @Override
    @NonNull
    public Task<BaseCard> updateCard(@NonNull BaseCard oldCard, @NonNull String[] text) {
        Card card = getOverloadedCard(oldCard);
        if (card == null) return Tasks.forCanceled();

        JSONObject obj;
        try {
            obj = card.toSyncJson();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating edit card payload.", ex);
            return Tasks.forException(ex);
        }

        return OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.EDIT_CARD, obj, null, null)
                .continueWith((Continuation<OverloadedSyncApi.CollaboratorPatchResponse, BaseCard>) task -> {
                    String textJoin = CommonUtils.join(text, "____");
                    Card overloadedCard = card.update(textJoin);
                    return ((ContentCard) oldCard).update(overloadedCard, textJoin);
                })
                .addOnFailureListener(ex -> Log.e(TAG, "Failed updating card.", ex));
    }

    @Override
    @NonNull
    public Task<BaseCard> addCard(boolean black, @NonNull String[] text) {
        JSONObject card;
        try {
            card = Card.toSyncJson(black, text);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating add card payload.", ex);
            return Tasks.forException(ex);
        }

        return OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.ADD_CARD, card, null, null)
                .continueWith((Continuation<OverloadedSyncApi.CollaboratorPatchResponse, BaseCard>) task -> {
                    OverloadedSyncApi.CollaboratorPatchResponse result = task.getResult();
                    if (result.cardId == null)
                        throw new IllegalArgumentException("Missing cardId in response.");

                    String textJoin = CommonUtils.join(text, "____");
                    Card overloadedCard = Card.from(textJoin, watermark, black, result.cardId);
                    return new ContentCard(overloadedCard, textJoin, watermark, black);
                })
                .addOnFailureListener(ex -> Log.e(TAG, "Failed adding card.", ex));
    }

    @Override
    @NotNull
    public Task<List<? extends BaseCard>> addCards(@NonNull boolean[] blacks, @NonNull String[][] texts) {
        JSONArray array = new JSONArray();
        try {
            for (int i = 0; i < blacks.length; i++)
                array.put(Card.toSyncJson(blacks[i], texts[i]));
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating add cards payload.", ex);
            return Tasks.forException(ex);
        }

        return OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.ADD_CARDS, null, null, array)
                .continueWith((Continuation<OverloadedSyncApi.CollaboratorPatchResponse, List<? extends BaseCard>>) task -> {
                    OverloadedSyncApi.CollaboratorPatchResponse result = task.getResult();
                    if (result.cardsIds == null)
                        throw new IllegalArgumentException("Missing cardsIds in response.");

                    if (result.cardsIds.length != blacks.length)
                        throw new IllegalArgumentException(String.format("IDs size mismatch, remote: %d, local: %d", result.cardsIds.length, blacks.length));

                    List<ContentCard> cards = new ArrayList<>(result.cardsIds.length);
                    for (int i = 0; i < result.cardsIds.length; i++) {
                        String textJoin = CommonUtils.join(texts[i], "____");
                        Card overloadedCard = Card.from(textJoin, watermark, blacks[i], result.cardsIds[i]);
                        cards.add(new ContentCard(overloadedCard, textJoin, watermark, blacks[i]));
                    }

                    return cards;
                })
                .addOnFailureListener(ex -> Log.e(TAG, "Failed adding cards.", ex));
    }
}
