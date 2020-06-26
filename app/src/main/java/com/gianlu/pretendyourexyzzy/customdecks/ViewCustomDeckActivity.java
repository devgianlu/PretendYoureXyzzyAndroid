package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.view.BlackCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.view.GeneralInfoFragment;
import com.gianlu.pretendyourexyzzy.customdecks.view.WhiteCardsFragment;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.UserProfile.CustomDeckWithCards;

public class ViewCustomDeckActivity extends ActivityWithDialog {
    private static final String TAG = ViewCustomDeckActivity.class.getSimpleName();
    private ViewPager pager;

    public static void startActivity(@NotNull Context context, @NotNull String username, @NotNull String deckName) {
        Intent intent = new Intent(context, ViewCustomDeckActivity.class);
        intent.putExtra("owner", username);
        intent.putExtra("deckName", deckName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_view_custom_deck);
        setTitle(getIntent().getStringExtra("deckName") + " - " + getIntent().getStringExtra("owner"));
        setSupportActionBar(findViewById(R.id.editViewCustomDeck_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        ProgressBar loading = findViewById(R.id.editViewCustomDeck_loading);
        loading.setVisibility(View.VISIBLE);

        String owner = getIntent().getStringExtra("owner");
        String deckName = getIntent().getStringExtra("deckName");
        if (owner == null || deckName == null) {
            onBackPressed();
            return;
        }

        pager = findViewById(R.id.editViewCustomDeck_pager);
        pager.setOffscreenPageLimit(3);
        TabLayout tabs = findViewById(R.id.editViewCustomDeck_tabs);
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

        OverloadedSyncApi.get().getPublicCustomDeck(owner, deckName, this, new GeneralCallback<CustomDeckWithCards>() {
            @Override
            public void onResult(@NonNull CustomDeckWithCards result) {
                pager.setAdapter(new PagerAdapter(getSupportFragmentManager(),
                        GeneralInfoFragment.get(ViewCustomDeckActivity.this, result.name, result.watermark, result.desc),
                        BlackCardsFragment.get(ViewCustomDeckActivity.this, result.blackCards()),
                        WhiteCardsFragment.get(ViewCustomDeckActivity.this, result.whiteCards())));
                tabs.setupWithViewPager(pager);

                loading.setVisibility(View.GONE);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed loading custom deck cards.", ex);
                Toaster.with(ViewCustomDeckActivity.this).message(R.string.failedLoading);
                onBackPressed();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
