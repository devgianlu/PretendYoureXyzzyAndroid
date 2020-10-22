package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.OverloadedApi.OverloadedServerException;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

public class NewViewCustomDeckActivity extends AbsNewCustomDeckActivity {
    private static final String TAG = NewViewCustomDeckActivity.class.getSimpleName();

    @NotNull
    private static Intent baseStartIntent(@NotNull Context context, @NotNull Type type) {
        Intent intent = new Intent(context, NewViewCustomDeckActivity.class);
        intent.putExtra("type", type);
        return intent;
    }

    @NotNull
    public static Intent activitySearchIntent(@NonNull Context context, @NonNull Deck deck) {
        Intent intent = baseStartIntent(context, Type.SEARCH);
        intent.putExtra("deckName", deck.name);
        intent.putExtra("watermark", deck.watermark);
        intent.putExtra("desc", deck.description);
        intent.putExtra("blackCards", deck.blackCards);
        intent.putExtra("whiteCards", deck.whiteCards);
        return intent;
    }

    @NonNull
    public static Intent activityCrCastIntent(@NotNull Context context, @NonNull CrCastDeck deck) {
        Intent intent = baseStartIntent(context, Type.CR_CAST);
        intent.putExtra("deckCode", deck.watermark);
        intent.putExtra("deckName", deck.name);
        intent.putExtra("favorite", deck.favorite);
        return intent;
    }

    @NonNull
    public static Intent activityPublicIntent(@NotNull Context context, @NotNull CustomDecksDatabase.StarredDeck deck) {
        Intent intent = baseStartIntent(context, Type.PUBLIC);
        intent.putExtra("owner", deck.owner);
        intent.putExtra("shareCode", deck.shareCode);
        intent.putExtra("deckName", deck.name);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBottomButtonMode(Mode.HIDDEN);

        Type type = (Type) getIntent().getSerializableExtra("type");
        if (type == null) return;

        CustomDecksDatabase db = CustomDecksDatabase.get(this);

        switch (type) {
            case SEARCH:
                String deckName = getIntent().getStringExtra("deckName");
                String watermark = getIntent().getStringExtra("watermark");
                String desc = getIntent().getStringExtra("desc");
                int blackCards = getIntent().getIntExtra("blackCards", -1);
                int whiteCards = getIntent().getIntExtra("whiteCards", -1);

                if (deckName == null || watermark == null || desc == null || blackCards == -1 || whiteCards == -1)
                    return;

                OverloadedSyncApi.get().searchPublicCustomDeck(deckName, watermark, desc, blackCards, whiteCards)
                        .addOnSuccessListener(this::deckLoaded)
                        .addOnFailureListener(ex -> {
                            Log.e(TAG, "Failed searching custom deck.", ex);

                            if (ex instanceof OverloadedServerException && (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_DECK)
                                    || ((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_USER))) {
                                Toaster.with(NewViewCustomDeckActivity.this).message(R.string.cannotFindCustomDeck).show();
                            } else {
                                Toaster.with(NewViewCustomDeckActivity.this).message(R.string.failedLoading).show();
                            }

                            onBackPressed();
                        });
                break;
            case CR_CAST:
                String deckCode = getIntent().getStringExtra("deckCode");
                if (deckCode == null)
                    return;

                CrCastApi.get().getDeck(deckCode, getIntent().getBooleanExtra("favorite", true), db)
                        .addOnSuccessListener(this::deckLoaded)
                        .addOnFailureListener(ex -> {
                            Log.e(TAG, "Failed loading CrCast deck.", ex);
                            Toaster.with(NewViewCustomDeckActivity.this).message(R.string.failedLoading).show();
                            onBackPressed();
                        });
                break;
            case PUBLIC:
                String shareCode = getIntent().getStringExtra("shareCode");
                String owner = getIntent().getStringExtra("owner");
                deckName = getIntent().getStringExtra("deckName");
                if (owner == null || deckName == null || shareCode == null)
                    return;

                OverloadedSyncApi.get().getPublicCustomDeck(owner, deckName)
                        .addOnSuccessListener(result -> {
                            deckLoaded(result);
                            db.updateStarredDeck(result.shareCode, result.name, result.watermark, result.count);
                        })
                        .addOnFailureListener(ex -> {
                            Log.e(TAG, "Failed loading custom deck cards.", ex);

                            if (ex instanceof OverloadedServerException && (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_DECK)
                                    || ((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_USER))) {
                                db.removeStarredDeck(owner, shareCode);
                                Toaster.with(NewViewCustomDeckActivity.this).message(R.string.deckDoesNotExist).show();
                            } else {
                                Toaster.with(NewViewCustomDeckActivity.this).message(R.string.failedLoading).show();
                            }

                            onBackPressed();
                        });
                break;
            default:
                throw new IllegalArgumentException(type.name());
        }
    }

    private void deckLoaded(@NotNull CrCastDeck deck) {
        // TODO: Load deck
    }

    private void deckLoaded(@NotNull UserProfile.CustomDeckWithCards deck) {
        // TODO: Load deck
    }

    @NotNull
    @Override
    protected Fragment getNewFragmentFor(int pos) {
        return pos == 0 ? new InfoFragment() : pos == 1 ? new BlacksFragment() : new WhitesFragment();
    }

    private enum Type {
        SEARCH, CR_CAST, PUBLIC
    }

    public static class InfoFragment extends FragmentWithDialog {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    public static class BlacksFragment extends AbsNewCardsFragment {

        @Override
        protected boolean addEnabled() {
            return false;
        }
    }

    public static class WhitesFragment extends AbsNewCardsFragment {

        @Override
        protected boolean addEnabled() {
            return false;
        }
    }
}
