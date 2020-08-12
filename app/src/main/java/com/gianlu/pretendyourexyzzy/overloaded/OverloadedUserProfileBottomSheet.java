package com.gianlu.pretendyourexyzzy.overloaded;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.typography.MaterialColors;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.ViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.main.OverloadedFragment;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

public final class OverloadedUserProfileBottomSheet extends ThemedModalBottomSheet<String, UserProfile> implements CardsAdapter.Listener, CustomDecksAdapter.Listener {
    private static final String TAG = OverloadedUserProfileBottomSheet.class.getSimpleName();
    private RecyclerView starredCards;
    private MessageView starredCardsMessage;
    private RecyclerView customDecks;
    private MessageView customDecksMessage;
    private SuperTextView cardsPlayed;
    private SuperTextView roundsPlayed;
    private SuperTextView roundsWon;
    private UserProfile lastPayload = null;

    @NonNull
    public static OverloadedUserProfileBottomSheet get() {
        return new OverloadedUserProfileBottomSheet();
    }

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView header, @NonNull String payload) {
        header.setTitle(payload);
        header.setBackgroundColorRes(MaterialColors.getShuffledInstance().next());
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull String payload) {
        inflater.inflate(R.layout.sheet_overloaded_user, parent, true);

        starredCardsMessage = parent.findViewById(R.id.overloadedUserSheet_starredCardsMessage);
        starredCardsMessage.info(R.string.userNoPublicStarredCards);

        starredCards = parent.findViewById(R.id.overloadedUserSheet_starredCards);
        starredCards.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));

        customDecksMessage = parent.findViewById(R.id.overloadedUserSheet_customDecksMessage);
        customDecksMessage.info(R.string.userNoPublicCustomDecksMessage);

        customDecks = parent.findViewById(R.id.overloadedUserSheet_customDecks);
        customDecks.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        cardsPlayed = parent.findViewById(R.id.overloadedUserSheet_cardsPlayed);
        roundsPlayed = parent.findViewById(R.id.overloadedUserSheet_roundsPlayed);
        roundsWon = parent.findViewById(R.id.overloadedUserSheet_roundsWon);

        isLoading(true);
        OverloadedApi.get().getProfile(payload, getActivity(), new GeneralCallback<UserProfile>() {
            @Override
            public void onResult(@NonNull UserProfile result) {
                update(result);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                dismissAllowingStateLoss();
                Log.e(TAG, "Failed loading user profile.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoadingUserProfile));
            }
        });
    }

    @Override
    protected void onReceivedUpdate(@NonNull UserProfile payload) {
        lastPayload = payload;
        isLoading(false);

        OverloadedFragment.setEventCount(payload.cardsPlayed, cardsPlayed, R.string.overloadedHeader_cardsPlayed);
        OverloadedFragment.setEventCount(payload.roundsPlayed, roundsPlayed, R.string.overloadedHeader_roundsPlayed);
        OverloadedFragment.setEventCount(payload.roundsWon, roundsWon, R.string.overloadedHeader_roundsWon);

        if (payload.starredCards.isEmpty()) {
            starredCards.setVisibility(View.GONE);
            starredCardsMessage.setVisibility(View.VISIBLE);
        } else {
            starredCards.setVisibility(View.VISIBLE);
            starredCardsMessage.setVisibility(View.GONE);
            starredCards.setAdapter(new CardsAdapter(false, StarredCardsDatabase.transform(payload.starredCards), GameCardView.Action.TOGGLE_STAR, null, true, this));
        }

        if (payload.customDecks.isEmpty()) {
            customDecks.setVisibility(View.GONE);
            customDecksMessage.setVisibility(View.VISIBLE);
        } else {
            customDecks.setVisibility(View.VISIBLE);
            customDecksMessage.setVisibility(View.GONE);
            customDecks.setAdapter(new CustomDecksAdapter(requireContext(), CustomDecksDatabase.transform(getSetupPayload(), payload.customDecks), this));
        }
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull String payload) {
        return false;
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (action == GameCardView.Action.TOGGLE_STAR) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_STARRED_CARD_ADD);

            if (StarredCardsDatabase.get(requireContext()).putCard((StarredCardsDatabase.FloatingStarredCard) card))
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.addedCardToStarred));
        }
    }

    @Override
    public void onCustomDeckSelected(@NonNull BasicCustomDeck deck) {
        if (lastPayload == null) return;

        UserProfile.CustomDeck userDeck = UserProfile.CustomDeck.find(lastPayload.customDecks, deck.name);
        if (userDeck == null) return;

        ViewCustomDeckActivity.startActivity(requireContext(), getSetupPayload(), deck.name, userDeck.shareCode);
    }
}
