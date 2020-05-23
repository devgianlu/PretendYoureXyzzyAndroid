package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.edit.BlackCardsFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.GeneralInfoFragment;
import com.gianlu.pretendyourexyzzy.customdecks.edit.WhiteCardsFragment;
import com.google.android.material.tabs.TabLayout;

public class EditCustomDeckActivity extends ActivityWithDialog {
    private GeneralInfoFragment generalInfoFragment;
    private ViewPager pager;

    public static void startActivityNew(@NonNull Context context) {
        context.startActivity(new Intent(context, EditCustomDeckActivity.class)
                .putExtra("title", context.getString(R.string.createCustomDeck)));
    }

    public static void startActivityEdit(@NonNull Context context, @NonNull CustomDecksDatabase.CustomDeck deck) {
        context.startActivity(new Intent(context, EditCustomDeckActivity.class)
                .putExtra("title", deck.name + " - " + context.getString(R.string.editCustomDeck))
                .putExtra("id", deck.id));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_custom_deck, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_custom_deck);
        setTitle(getIntent().getStringExtra("title"));
        setSupportActionBar(findViewById(R.id.editCustomDeck_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        Integer id = getIntent().getIntExtra("id", -1);
        if (id == -1) id = null;

        pager = findViewById(R.id.editCustomDeck_pager);
        pager.setOffscreenPageLimit(3);
        TabLayout tabs = findViewById(R.id.editCustomDeck_tabs);
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

        pager.setAdapter(new PagerAdapter(getSupportFragmentManager(),
                generalInfoFragment = GeneralInfoFragment.get(this, id),
                BlackCardsFragment.get(this, id),
                WhiteCardsFragment.get(this, id)));
        tabs.setupWithViewPager(pager);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.editCustomDeck_done:
                if (generalInfoFragment != null && generalInfoFragment.save()) {
                    onBackPressed();
                } else if (pager != null) {
                    pager.setCurrentItem(0);
                }
                return true;
            default:
                return false;
        }
    }
}
