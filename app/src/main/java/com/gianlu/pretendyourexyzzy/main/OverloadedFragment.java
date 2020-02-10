package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedApi;
import com.gianlu.pretendyourexyzzy.overloaded.fragments.ChatsFragment;
import com.gianlu.pretendyourexyzzy.overloaded.fragments.ProfileFragment;
import com.google.android.material.tabs.TabLayout;

public class OverloadedFragment extends FragmentWithDialog {

    @NonNull
    public static OverloadedFragment getInstance() {
        return new OverloadedFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_overloaded, container, false);

        OverloadedApi.UserData data = OverloadedApi.get().userDataCache();

        TextView username = layout.findViewById(R.id.overloaded_username);
        if (data != null) username.setText(data.username);

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
