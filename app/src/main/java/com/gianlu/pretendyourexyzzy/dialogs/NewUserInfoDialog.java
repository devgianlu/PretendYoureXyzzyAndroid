package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.NewCustomDecksAdapter;
import com.gianlu.pretendyourexyzzy.adapters.NewStarredCardsAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.WhoisResult;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.databinding.DialogNewUserInfoBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.UserProfile;

public final class NewUserInfoDialog extends DialogFragment {
    private static final String TAG = NewUserInfoDialog.class.getSimpleName();
    private RegisteredPyx pyx;
    private WhoisResult pyxUser;
    private UserProfile overloadedUser;

    @NotNull
    public static NewUserInfoDialog get(@NotNull String username, boolean whois) {
        NewUserInfoDialog dialog = new NewUserInfoDialog();
        Bundle args = new Bundle();
        args.putBoolean("whois", whois);
        args.putString("username", username);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        Window window;
        if (dialog != null && (window = dialog.getWindow()) != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogNewUserInfoBinding binding = DialogNewUserInfoBinding.inflate(inflater, container, false);
        binding.getRoot().setOnClickListener(v -> dismissAllowingStateLoss());
        binding.userInfoDialogClose.setOnClickListener(v -> dismissAllowingStateLoss());

        String username = requireArguments().getString("username");
        if (username == null) {
            dismissAllowingStateLoss();
            return null;
        }

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            dismissAllowingStateLoss();
            return null;
        }

        binding.userInfoDialogUsername.setText(username);

        //region PYX
        boolean whois = requireArguments().getBoolean("whois");
        if (whois) {
            binding.userInfoDialogPyxInfo.setVisibility(View.GONE);
            binding.userInfoDialogPyxInfoLoading.setVisibility(View.VISIBLE);
            binding.userInfoDialogPyxInfoLoading.showShimmer(true);

            pyx.request(PyxRequests.whois(username))
                    .addOnSuccessListener(result -> {
                        this.pyxUser = result;

                        long onlineFor = System.currentTimeMillis() - result.connectedAt;
                        binding.userInfoDialogOnlineFor.setText(CommonUtils.timeFormatter(onlineFor / 1000));

                        Game game = result.game();
                        if (game != null) {
                            binding.userInfoDialogGame.setTypeface(binding.userInfoDialogGame.getTypeface(), Typeface.NORMAL);
                            binding.userInfoDialogGame.setText(game.host);
                            // TODO: View game (?)
                        } else {
                            binding.userInfoDialogGame.setTypeface(binding.userInfoDialogGame.getTypeface(), Typeface.ITALIC);
                            binding.userInfoDialogGame.setText(R.string.none);
                        }

                        binding.userInfoDialogPyxInfoLoading.setVisibility(View.GONE);
                        binding.userInfoDialogPyxInfo.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed loading whois info.", ex);

                        if (!OverloadedUtils.isSignedIn() || overloadedUser == null)
                            dismissAllowingStateLoss();
                    });
        } else {
            binding.userInfoDialogPyxInfo.setVisibility(View.GONE);
            binding.userInfoDialogPyxInfoLoading.setVisibility(View.GONE);
        }
        //endregion

        //region Overloaded
        if (OverloadedUtils.isSignedIn()) {
            binding.userInfoDialogAddFriend.setVisibility(View.GONE);
            binding.userInfoDialogOverloadedInfo.setVisibility(View.GONE);
            binding.userInfoDialogOverloadedInfoLoading.setVisibility(View.VISIBLE);
            binding.userInfoDialogOverloadedInfoLoading.showShimmer(true);

            binding.userInfoDialogCustomDecks.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
            binding.userInfoDialogStarredCards.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));

            OverloadedApi.get().getProfile(username)
                    .addOnSuccessListener(profile -> {
                        this.overloadedUser = profile;

                        // TODO: Open decks
                        // TODO: Save starred cards

                        if (profile.customDecks.isEmpty()) {
                            binding.userInfoDialogCustomDecksEmpty.setVisibility(View.VISIBLE);
                            binding.userInfoDialogCustomDecks.setVisibility(View.GONE);
                        } else {
                            binding.userInfoDialogCustomDecksEmpty.setVisibility(View.GONE);
                            binding.userInfoDialogCustomDecks.setVisibility(View.VISIBLE);
                            binding.userInfoDialogCustomDecks.setAdapter(new NewCustomDecksAdapter(requireContext(), BasicCustomDeck.fromOverloadedDecks(profile.customDecks, username), null));
                        }

                        if (profile.starredCards.isEmpty()) {
                            binding.userInfoDialogStarredCardsEmpty.setVisibility(View.VISIBLE);
                            binding.userInfoDialogStarredCards.setVisibility(View.GONE);
                        } else {
                            binding.userInfoDialogStarredCardsEmpty.setVisibility(View.GONE);
                            binding.userInfoDialogStarredCards.setVisibility(View.VISIBLE);
                            binding.userInfoDialogStarredCards.setAdapter(new NewStarredCardsAdapter(requireContext(), null, StarredCard.fromOverloadedCards(profile.starredCards)));
                        }

                        binding.userInfoDialogOverloadedInfo.setVisibility(View.VISIBLE);
                        binding.userInfoDialogOverloadedInfoLoading.setVisibility(View.GONE);

                        if (OverloadedApi.get().hasFriendCached(username)) {
                            binding.userInfoDialogAddFriend.setVisibility(View.GONE);
                        } else {
                            binding.userInfoDialogAddFriend.setVisibility(View.VISIBLE);
                            binding.userInfoDialogAddFriend.setEnabled(true);
                            binding.userInfoDialogAddFriend.setOnClickListener(v -> {
                                binding.userInfoDialogAddFriend.setEnabled(false);
                                OverloadedApi.get().addFriend(username)
                                        .addOnSuccessListener(stringFriendStatusMap -> {
                                            DialogUtils.showToast(getActivity(), Toaster.build().message(R.string.friendAdded).extra(username));
                                            binding.userInfoDialogAddFriend.setVisibility(View.GONE);
                                        })
                                        .addOnFailureListener(ex -> {
                                            Log.e(TAG, "Failed adding friend.", ex);
                                            DialogUtils.showToast(getActivity(), Toaster.build().message(R.string.failedAddingFriend).extra(username));
                                            binding.userInfoDialogAddFriend.setEnabled(true);
                                        });
                            });
                        }
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed loading Overloaded profile.", ex);

                        binding.userInfoDialogAddFriend.setVisibility(View.GONE);
                        binding.userInfoDialogOverloadedInfo.setVisibility(View.GONE);
                        binding.userInfoDialogOverloadedInfoLoading.setVisibility(View.GONE);

                        if (!whois || pyxUser == null)
                            dismissAllowingStateLoss();
                    });
        } else {
            binding.userInfoDialogAddFriend.setVisibility(View.GONE);
            binding.userInfoDialogOverloadedInfo.setVisibility(View.GONE);
            binding.userInfoDialogOverloadedInfoLoading.setVisibility(View.GONE);
        }
        //endregion

        return binding.getRoot();
    }

    private static class StarredCard extends BaseCard {
        private final String text;

        StarredCard(@NotNull UserProfile.StarredCard card) {
            String[] whiteTexts = new String[card.whiteCards.length];
            for (int i = 0; i < card.whiteCards.length; i++)
                whiteTexts[i] = card.whiteCards[i].text;
            text = StarredCardsDatabase.createSentence(card.blackCard.text, whiteTexts);
        }

        @NotNull
        static List<StarredCard> fromOverloadedCards(@NonNull List<UserProfile.StarredCard> cards) {
            List<StarredCard> list = new ArrayList<>(cards.size());
            for (UserProfile.StarredCard card : cards) list.add(new StarredCard(card));
            return list;
        }

        @NonNull
        @Override
        public String text() {
            return text;
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
}
