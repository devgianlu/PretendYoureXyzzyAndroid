package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.overloaded.api.UserDataCallback;
import com.gianlu.pretendyourexyzzy.overloaded.fragments.ChatsFragment;
import com.gianlu.pretendyourexyzzy.overloaded.fragments.ProfileFragment;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.event.Event;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OverloadedFragment extends FragmentWithDialog {
    private LinearLayout achievements;
    private SuperTextView cardsPlayed;
    private SuperTextView roundsPlayed;
    private SuperTextView roundsWon;

    @NonNull
    public static OverloadedFragment getInstance() {
        return new OverloadedFragment();
    }

    private static void populateAchievementsLayout(@NonNull LinearLayout layout, @NonNull List<Achievement> achievements) {
        layout.removeAllViews();
        ImageManager im = ImageManager.create(layout.getContext());
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, layout.getContext().getResources().getDisplayMetrics());

        for (Achievement ach : achievements) {
            ImageView view = new ImageView(layout.getContext());
            im.loadImage(view, ach.getUnlockedImageUri());
            layout.addView(view, new LinearLayout.LayoutParams(size, size));

            if (!Objects.equals(ach.getAchievementId(), GPGamesHelper.ACH_CARDCAST))
                view.setPadding(24, 24, 24, 24);
        }
    }

    private static void setEventCount(@NonNull Iterable<Event> events, @NonNull String eventId, @NonNull SuperTextView view, @StringRes int titleRes) {
        Event event = OverloadedUtils.findEvent(events, eventId);
        String formattedValue;
        if (event == null) {
            formattedValue = "??";
        } else {
            long value = event.getValue();
            if (value <= 10000) formattedValue = String.valueOf(value);
            else formattedValue = String.format(Locale.getDefault(), "%.2fK", value / 1000f);
        }

        view.setHtml(titleRes, formattedValue);
    }

    @Override
    public void onResume() {
        super.onResume();

        GPGamesHelper.loadAchievements(requireContext(), getActivity(), new GPGamesHelper.LoadIterable<Achievement>() {
            @Override
            public void onLoaded(@NonNull Iterable<Achievement> result) {
                if (!result.iterator().hasNext()) {
                    achievements.setVisibility(View.GONE);
                    return;
                }

                achievements.setVisibility(View.VISIBLE);
                populateAchievementsLayout(achievements, OverloadedUtils.getBestAchievements(result));
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Logging.log(ex);
                onLoaded(Collections.emptyList());
            }
        });

        GPGamesHelper.loadEvents(requireContext(), getActivity(), new GPGamesHelper.LoadIterable<Event>() {
            @Override
            public void onLoaded(@NonNull Iterable<Event> result) {
                setEventCount(result, GPGamesHelper.EVENT_CARDS_PLAYED, cardsPlayed, R.string.overloadedHeader_cardsPlayed);
                setEventCount(result, GPGamesHelper.EVENT_ROUNDS_PLAYED, roundsPlayed, R.string.overloadedHeader_roundsPlayed);
                setEventCount(result, GPGamesHelper.EVENT_ROUNDS_WON, roundsWon, R.string.overloadedHeader_roundsWon);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Logging.log(ex);
                onLoaded(Collections.emptyList());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_overloaded, container, false);

        achievements = layout.findViewById(R.id.overloaded_achievements);
        cardsPlayed = layout.findViewById(R.id.overloaded_cardsPlayed);
        roundsPlayed = layout.findViewById(R.id.overloaded_roundsPlayed);
        roundsWon = layout.findViewById(R.id.overloaded_roundsWon);

        TextView username = layout.findViewById(R.id.overloaded_username);
        OverloadedApi.get().userData(getActivity(), true, new UserDataCallback() {
            @Override
            public void onUserData(@NonNull OverloadedApi.UserData data) {
                username.setText(data.username);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Logging.log(ex); // Will hardly fail
            }
        });

        TabLayout tabs = layout.findViewById(R.id.overloaded_tabs);
        ViewPager pager = layout.findViewById(R.id.overloaded_pager);
        pager.setOffscreenPageLimit(3);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        pager.setAdapter(new PagerAdapter(getChildFragmentManager(),
                ProfileFragment.get(requireContext()),
                ChatsFragment.get(requireContext())));

        tabs.setupWithViewPager(pager);
        return layout;
    }
}
