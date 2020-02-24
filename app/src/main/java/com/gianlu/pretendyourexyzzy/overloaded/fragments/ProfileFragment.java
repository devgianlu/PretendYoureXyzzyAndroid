package com.gianlu.pretendyourexyzzy.overloaded.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.ImagesListView;
import com.gianlu.pretendyourexyzzy.overloaded.AchievementImageLoader;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.overloaded.api.SuccessfulCallback;
import com.gianlu.pretendyourexyzzy.overloaded.api.UserDataCallback;
import com.google.android.gms.games.achievement.Achievement;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class ProfileFragment extends FragmentWithDialog {
    private ImagesListView achievements;
    private ImagesListView linkedAccounts;
    private LinearLayout friends;

    @NonNull
    public static ProfileFragment get(@NonNull Context context) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.profile));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        GPGamesHelper.loadAchievements(requireContext(), getActivity(), new GPGamesHelper.LoadIterable<Achievement>() {
            @Override
            public void onLoaded(@NonNull Iterable<Achievement> result) {
                AchievementImageLoader il = new AchievementImageLoader(requireContext());

                achievements.removeAllViews();
                for (Achievement ach : OverloadedUtils.getUnlockedAchievements(result))
                    achievements.addItem(ach, il);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Logging.log(ex);
                onLoaded(Collections.emptyList());
            }
        });

        linkedAccounts.removeAllViews();
        for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS)
            if (OverloadedApi.get().hasLinkedProvider(provider.id))
                linkedAccounts.addItem(provider.iconRes, ImageView::setImageResource);

        OverloadedApi.get().userData(getActivity(), false, new UserDataCallback() {
            @Override
            public void onUserData(@NonNull OverloadedApi.UserData data) {
                friends.removeAllViews();
                for (String friend : data.friends)
                    friends.addView(SuperTextView.builder(requireContext()).text(friend).build());
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Logging.log(ex);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_overloaded_profile, container, false);
        achievements = layout.findViewById(R.id.overloadedProfileFragment_achievements);
        linkedAccounts = layout.findViewById(R.id.overloadedProfileFragment_linkedAccounts);
        friends = layout.findViewById(R.id.overloadedProfileFragment_friends);

        OverloadedApi.get().addFriend("devgianlu_emulator", getActivity(), new SuccessfulCallback() {
            @Override
            public void onSuccessful() {
                System.out.println("OK");
            }

            @Override
            public void onFailed(@NotNull Exception ex) {
                Logging.log(ex);
            }
        });

        return layout;
    }
}