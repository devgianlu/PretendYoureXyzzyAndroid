package com.gianlu.pretendyourexyzzy.customdecks;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.gianlu.pretendyourexyzzy.customdecks.AbsCardsFragment.CardActionCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi.CollaboratorPatchOp;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.Card;

public final class CollaboratorHandler implements AbsCardsFragment.CardActionsHandler {
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

    @Override
    public void removeCard(@NonNull BaseCard oldCard, @NonNull CardActionCallback<Void> callback) {
        Card card = getOverloadedCard(oldCard);
        if (card == null) return;

        OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.REM_CARD, null, card.remoteId,
                null, null, new GeneralCallback<OverloadedSyncApi.CollaboratorPatchResponse>() {
                    @Override
                    public void onResult(@NonNull OverloadedSyncApi.CollaboratorPatchResponse result) {
                        callback.onComplete(null);
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        Log.e(TAG, "Failed removing card.", ex);
                        callback.onFailed();
                    }
                });
    }

    @Override
    public void updateCard(@NonNull BaseCard oldCard, @NonNull String[] text, @NonNull CardActionCallback<BaseCard> callback) {
        Card card = getOverloadedCard(oldCard);
        if (card == null) return;

        JSONObject obj;
        try {
            obj = card.toSyncJson();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating edit card payload.", ex);
            callback.onFailed();
            return;
        }

        OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.EDIT_CARD, obj, null,
                null, null, new GeneralCallback<OverloadedSyncApi.CollaboratorPatchResponse>() {
                    @Override
                    public void onResult(@NonNull OverloadedSyncApi.CollaboratorPatchResponse result) {
                        String textJoin = CommonUtils.join(text, "____");
                        Card overloadedCard = card.update(textJoin);
                        ContentCard newCard = ((ContentCard) oldCard).update(overloadedCard, textJoin);
                        callback.onComplete(newCard);
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        Log.e(TAG, "Failed updating card.", ex);
                        callback.onFailed();
                    }
                });
    }

    @Override
    public void addCard(boolean black, @NonNull String[] text, @NonNull CardActionCallback<BaseCard> callback) {
        JSONObject card;
        try {
            card = Card.toSyncJson(black, text);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating add card payload.", ex);
            callback.onFailed();
            return;
        }

        OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.ADD_CARD, card, null,
                null, null, new GeneralCallback<OverloadedSyncApi.CollaboratorPatchResponse>() {
                    @Override
                    public void onResult(@NonNull OverloadedSyncApi.CollaboratorPatchResponse result) {
                        if (result.cardId == null) {
                            Log.e(TAG, "Missing cardId in response.");
                            callback.onFailed();
                            return;
                        }

                        String textJoin = CommonUtils.join(text, "____");
                        Card overloadedCard = Card.from(textJoin, watermark, black, result.cardId);
                        ContentCard newCard = new ContentCard(overloadedCard, textJoin, watermark, black);
                        callback.onComplete(newCard);
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        Log.e(TAG, "Failed adding card.", ex);
                        callback.onFailed();
                    }
                });
    }

    @Override
    public void addCards(@NonNull boolean[] blacks, @NonNull String[][] texts, @NonNull CardActionCallback<List<? extends BaseCard>> callback) {
        JSONArray array = new JSONArray();
        try {
            for (int i = 0; i < blacks.length; i++)
                array.put(Card.toSyncJson(blacks[i], texts[i]));
        } catch (JSONException ex) {
            Log.e(TAG, "Failed creating add cards payload.", ex);
            callback.onFailed();
            return;
        }

        OverloadedSyncApi.get().patchCollaborator(OverloadedApi.now(), shareCode, CollaboratorPatchOp.ADD_CARDS, null, null,
                array, null, new GeneralCallback<OverloadedSyncApi.CollaboratorPatchResponse>() {
                    @Override
                    public void onResult(@NonNull OverloadedSyncApi.CollaboratorPatchResponse result) {
                        if (result.cardsIds == null) {
                            Log.e(TAG, "Missing cardsIds in response.");
                            callback.onFailed();
                            return;
                        }

                        if (result.cardsIds.length != blacks.length) {
                            Log.e(TAG, String.format("IDs size mismatch, remote: %d, local: %d", result.cardsIds.length, blacks.length));
                            callback.onFailed();
                            return;
                        }

                        List<ContentCard> cards = new ArrayList<>(result.cardsIds.length);
                        for (int i = 0; i < result.cardsIds.length; i++) {
                            String textJoin = CommonUtils.join(texts[i], "____");
                            Card overloadedCard = Card.from(textJoin, watermark, blacks[i], result.cardsIds[i]);
                            cards.add(new ContentCard(overloadedCard, textJoin, watermark, blacks[i]));
                        }

                        callback.onComplete(cards);
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        Log.e(TAG, "Failed adding cards.", ex);
                        callback.onFailed();
                    }
                });
    }
}
