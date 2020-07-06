package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.main.chats.PyxChatFragment;
import com.google.android.material.tabs.TabLayout;

public class PyxChatsFragment extends Fragment {
    private PyxChatFragment globalFragment;
    private PyxChatFragment gameFragment;
    private ViewPager pager;
    private boolean gameEnabled;

    @NonNull
    public static PyxChatsFragment get(@NonNull RegisteredPyx pyx) {
        if (!pyx.config().globalChatEnabled() && !pyx.config().gameChatEnabled())
            throw new IllegalStateException();

        PyxChatsFragment fragment = new PyxChatsFragment();
        Bundle args = new Bundle();
        args.putBoolean("globalEnabled", pyx.config().globalChatEnabled());
        args.putBoolean("gameEnabled", pyx.config().gameChatEnabled());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_pyx_chats, container, false);

        Bundle args = requireArguments();
        boolean globalEnabled = args.getBoolean("globalEnabled");
        gameEnabled = args.getBoolean("gameEnabled");

        TabLayout tabs = layout.findViewById(R.id.pyxChatsFragment_tabs);
        pager = layout.findViewById(R.id.pyxChatsFragment_pager);
        pager.setOffscreenPageLimit(2);
        tabs.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                onSelectedFragment();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        if (globalEnabled) globalFragment = PyxChatFragment.getGlobalInstance(requireContext());
        pager.setAdapter(new PagerAdapter(getChildFragmentManager(), globalFragment));

        return layout;
    }

    public void scrollToTop() {
        if (gameFragment != null && gameFragment.isVisible()) {
            gameFragment.scrollToTop();
        } else if (globalFragment != null && globalFragment.isVisible()) {
            globalFragment.scrollToTop();
        } else {
            if (gameFragment != null) gameFragment.scrollToTop();
            if (globalFragment != null) globalFragment.scrollToTop();
        }
    }

    public void toggleGameChat(@Nullable Integer gid) {
        if (getContext() == null) return;

        if (!gameEnabled || gid == null) {
            if (gameFragment != null) {
                gameFragment = null;

                if (globalFragment != null)
                    pager.setAdapter(new PagerAdapter(getChildFragmentManager(), globalFragment));
            }
        } else {
            gameFragment = PyxChatFragment.getGameInstance(gid, getContext());
            if (globalFragment != null)
                pager.setAdapter(new PagerAdapter(getChildFragmentManager(), globalFragment, gameFragment));
            else
                pager.setAdapter(new PagerAdapter(getChildFragmentManager(), gameFragment));
        }
    }

    public void onSelectedFragment() {
        PagerAdapter adapter;
        if (pager == null || (adapter = (PagerAdapter) pager.getAdapter()) == null)
            return;

        for (int i = 0; i < adapter.getCount(); i++) {
            PyxChatFragment fragment = ((PyxChatFragment) adapter.getItem(pager.getCurrentItem()));
            if (i == pager.getCurrentItem()) fragment.onSelectedFragment();
            else fragment.onDeselectedFragment();
        }
    }

    public void onDeselectedFragment() {
        PagerAdapter adapter;
        if (pager == null || (adapter = (PagerAdapter) pager.getAdapter()) == null)
            return;

        for (int i = 0; i < adapter.getCount(); i++)
            ((PyxChatFragment) adapter.getItem(i)).onDeselectedFragment();
    }
}
