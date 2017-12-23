package com.gianlu.pretendyourexyzzy.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.Arrays;
import java.util.List;

public class PagerAdapter extends FragmentStatePagerAdapter {
    private final List<Fragment> fragments;

    public PagerAdapter(FragmentManager fm, Fragment... fragments) {
        super(fm);
        this.fragments = Arrays.asList(fragments);
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getItem(position).getArguments().getString("title");
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}
