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

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewViewCustomDeckInfoBinding;
import com.gianlu.pretendyourexyzzy.dialogs.NewUserInfoDialog;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi.OverloadedServerException;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

public class NewViewCustomDeckActivity extends AbsNewCustomDeckActivity {
    private static final String TAG = NewViewCustomDeckActivity.class.getSimpleName();
    private String deckName;

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

    @NonNull
    public static Intent activityPublicIntent(@NotNull Context context, @NotNull NewUserInfoDialog.OverloadedCustomDecks deck) {
        Intent intent = baseStartIntent(context, Type.PUBLIC);
        intent.putExtra("owner", deck.owner);
        intent.putExtra("shareCode", deck.shareCode);
        intent.putExtra("deckName", deck.name);
        return intent;
    }

    @NotNull
    @Override
    protected String getName() {
        return deckName;
    }

    @Nullable
    @Override
    protected Integer getDeckId() {
        return null; // Not needed when viewing
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBottomButtonMode(Mode.HIDDEN);
        setMenuIconVisible(false);
        setPageChangeAllowed(true);

        deckName = getIntent().getStringExtra("deckName");
        Type type = (Type) getIntent().getSerializableExtra("type");
        if (type == null || deckName == null) return;

        CustomDecksDatabase db = CustomDecksDatabase.get(this);

        switch (type) {
            case SEARCH:
                String watermark = getIntent().getStringExtra("watermark");
                String desc = getIntent().getStringExtra("desc");
                int blackCards = getIntent().getIntExtra("blackCards", -1);
                int whiteCards = getIntent().getIntExtra("whiteCards", -1);

                if (watermark == null || desc == null || blackCards == -1 || whiteCards == -1)
                    return;

                OverloadedSyncApi.get().searchPublicCustomDeck(deckName, watermark, desc, blackCards, whiteCards)
                        .addOnSuccessListener(this::deckLoaded)
                        .addOnFailureListener(ex -> {
                            Log.e(TAG, "Failed searching custom deck.", ex);

                            if (ex instanceof OverloadedServerException && (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_DECK)
                                    || ((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NO_SUCH_USER))) {
                                Toaster.with(this).message(R.string.cannotFindCustomDeck).show();
                            } else {
                                Toaster.with(this).message(R.string.failedLoading).show();
                            }

                            onBackPressed();
                        });
                break;
            case CR_CAST:
                String deckCode = getIntent().getStringExtra("deckCode");
                if (deckCode == null)
                    return;

                CrCastApi.get().getDeck(deckCode, db)
                        .continueWithTask(task -> task.getResult().getCards(db))
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
                if (owner == null || shareCode == null)
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
                                db.removeStarredDeck(shareCode);
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
        CrCastDeck.Cards cards = deck.cards();
        if (cards == null) throw new IllegalArgumentException();

        loaded(InfoFragment.get(deck),
                BlacksFragment.get(cards.blacks, deck.watermark, null),
                WhitesFragment.get(cards.whites, deck.watermark, null));
    }

    private void deckLoaded(@NotNull UserProfile.CustomDeckWithCards deck) {
        CollaboratorHandler handler = null;
        if (deck.collaborator) handler = new CollaboratorHandler(deck.shareCode, deck.watermark);

        loaded(InfoFragment.get(deck),
                BlacksFragment.get(ContentCard.fromOverloadedCards(deck.blackCards()), deck.watermark, handler),
                WhitesFragment.get(ContentCard.fromOverloadedCards(deck.whiteCards()), deck.watermark, handler));
    }

    private enum Type {
        SEARCH, CR_CAST, PUBLIC
    }

    public static class InfoFragment extends FragmentWithDialog {
        private FragmentNewViewCustomDeckInfoBinding binding;

        @NotNull
        public static InfoFragment get(@NotNull UserProfile.CustomDeckWithCards deck) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putString("name", deck.name);
            args.putString("watermark", deck.watermark);
            args.putString("desc", deck.desc);
            args.putString("shareCode", deck.shareCode);
            args.putString("owner", deck.owner);
            args.putInt("blacks", deck.blackCards().size());
            args.putInt("whites", deck.whiteCards().size());
            args.putBoolean("canCollaborate", deck.collaborator);
            args.putBoolean("crCast", false);
            fragment.setArguments(args);
            return fragment;
        }

        @NotNull
        public static InfoFragment get(@NotNull CrCastDeck deck) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putString("name", deck.name);
            args.putString("watermark", deck.watermark);
            args.putString("desc", deck.desc);
            args.putString("shareCode", null);
            args.putString("owner", null);
            args.putInt("blacks", deck.blackCardsCount());
            args.putInt("whites", deck.whiteCardsCount());
            args.putBoolean("crCast", true);
            args.putBoolean("private", deck.privateDeck);
            args.putString("language", deck.lang);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentNewViewCustomDeckInfoBinding.inflate(inflater, container, false);

            binding.viewCustomDeckInfoName.setText(requireArguments().getString("name"));
            binding.viewCustomDeckInfoWatermark.setText(requireArguments().getString("watermark"));
            binding.viewCustomDeckInfoDesc.setText(requireArguments().getString("desc"));

            binding.viewCustomDeckBlackCards.setText(String.valueOf(requireArguments().getInt("blacks")));
            binding.viewCustomDeckWhiteCards.setText(String.valueOf(requireArguments().getInt("whites")));

            updateStar();

            if (requireArguments().getBoolean("crCast", false)) {
                ((View) binding.viewCustomDeckInfoCanCollaborate.getParent()).setVisibility(View.GONE);

                ((View) binding.viewCustomDeckInfoPrivateDeck.getParent()).setVisibility(View.VISIBLE);
                binding.viewCustomDeckInfoPrivateDeck.setText(requireArguments().getBoolean("private") ? R.string.yes : R.string.no);

                String lang = requireArguments().getString("language", "");
                if (lang.isEmpty() || lang.equals("-")) {
                    ((View) binding.viewCustomDeckInfoLanguage.getParent()).setVisibility(View.GONE);
                } else {
                    ((View) binding.viewCustomDeckInfoLanguage.getParent()).setVisibility(View.VISIBLE);
                    binding.viewCustomDeckInfoLanguage.setText(lang);
                }
            } else {
                ((View) binding.viewCustomDeckInfoPrivateDeck.getParent()).setVisibility(View.GONE);
                ((View) binding.viewCustomDeckInfoLanguage.getParent()).setVisibility(View.GONE);
                ((View) binding.viewCustomDeckInfoCanCollaborate.getParent()).setVisibility(View.VISIBLE);
                binding.viewCustomDeckInfoCanCollaborate.setText(requireArguments().getBoolean("canCollaborate") ? R.string.yes : R.string.no);
            }

            return binding.getRoot();
        }

        private void updateStar() {
            String shareCode = requireArguments().getString("shareCode");
            if (shareCode == null) {
                binding.viewCustomDeckInfoStar.setVisibility(View.GONE);
            } else {
                binding.viewCustomDeckInfoStar.setVisibility(View.VISIBLE);

                CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());
                if (db.isStarred(shareCode)) {
                    binding.viewCustomDeckInfoStar.setImageResource(R.drawable.baseline_star_24);
                    binding.viewCustomDeckInfoStar.setOnClickListener(v -> {
                        db.removeStarredDeck(shareCode);
                        updateStar();
                    });
                } else {
                    binding.viewCustomDeckInfoStar.setImageResource(R.drawable.baseline_star_outline_24);
                    binding.viewCustomDeckInfoStar.setOnClickListener(v -> {
                        String name = requireArguments().getString("name");
                        String watermark = requireArguments().getString("watermark");
                        String owner = requireArguments().getString("owner");
                        int cardsCount = requireArguments().getInt("blacks") + requireArguments().getInt("whites");
                        if (name == null || watermark == null || owner == null || cardsCount < 0)
                            return;

                        db.addStarredDeck(shareCode, name, watermark, owner, cardsCount);
                        updateStar();
                    });
                }
            }
        }
    }

    public static class BlacksFragment extends AbsNewCardsFragment {
        private List<? extends BaseCard> cards;

        @NotNull
        public static BlacksFragment get(List<? extends BaseCard> cards, @NotNull String watermark, @Nullable CardActionsHandler handler) {
            BlacksFragment fragment = new BlacksFragment();
            fragment.cards = cards;
            fragment.setHandler(handler);

            Bundle args = new Bundle();
            args.putString("watermark", watermark);
            fragment.setArguments(args);
            return fragment;
        }

        @NotNull
        @Override
        protected String getWatermark() {
            return requireArguments().getString("watermark", "");
        }

        @Override
        protected boolean isBlack() {
            return true;
        }

        @Override
        protected boolean canEditCards() {
            return handler != null;
        }

        @NotNull
        @Override
        protected List<? extends BaseCard> getCards(@NotNull Context context) {
            return cards;
        }
    }

    public static class WhitesFragment extends AbsNewCardsFragment {
        private List<? extends BaseCard> cards;

        @NotNull
        public static WhitesFragment get(List<? extends BaseCard> cards, @NotNull String watermark, @Nullable CardActionsHandler handler) {
            WhitesFragment fragment = new WhitesFragment();
            fragment.cards = cards;
            fragment.setHandler(handler);

            Bundle args = new Bundle();
            args.putString("watermark", watermark);
            fragment.setArguments(args);
            return fragment;
        }

        @NotNull
        @Override
        protected String getWatermark() {
            return requireArguments().getString("watermark", "");
        }

        @Override
        protected boolean isBlack() {
            return false;
        }

        @Override
        protected boolean canEditCards() {
            return handler != null;
        }

        @NotNull
        @Override
        protected List<? extends BaseCard> getCards(@NotNull Context context) {
            return cards;
        }
    }
}
