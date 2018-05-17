package com.gianlu.pretendyourexyzzy.SpareActivities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.CardcastDeck.CardsFragment;
import com.gianlu.pretendyourexyzzy.CardcastDeck.InfoFragment;
import com.gianlu.pretendyourexyzzy.Cards.StarredDecksManager;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameHelper;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class CardcastDeckActivity extends ActivityWithDialog {
    private String code;
    private String name;

    public static void startActivity(Context context, @NonNull CardcastDeck deck) {
        startActivity(context, deck.code, deck.name);
    }

    public static void startActivity(Context context, @NonNull String code, @NonNull String name) {
        context.startActivity(new Intent(context, CardcastDeckActivity.class)
                .putExtra("code", code)
                .putExtra("title", name));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cardcast_deck_info, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        OngoingGameHelper.Listener listener = OngoingGameHelper.get();
        menu.findItem(R.id.cardcastDeckInfo_add).setVisible(listener != null && listener.canModifyCardcastDecks());
        menu.findItem(R.id.cardcastDeckInfo_toggleStar).setIcon(StarredDecksManager.hasDeck(this, code) ? R.drawable.ic_star_white_48dp : R.drawable.ic_star_border_white_48dp);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.cardcastDeckInfo_add:
                OngoingGameHelper.Listener listener = OngoingGameHelper.get();
                if (listener != null && code != null) listener.addCardcastDeck(code);
                return true;
            case R.id.cardcastDeckInfo_toggleStar:
                if (StarredDecksManager.hasDeck(this, code))
                    StarredDecksManager.removeDeck(this, code);
                else
                    StarredDecksManager.addDeck(this, new StarredDecksManager.StarredDeck(code, name));

                invalidateOptionsMenu();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardcast_deck);
        setTitle(getIntent().getStringExtra("title"));

        setSupportActionBar((Toolbar) findViewById(R.id.cardcastDeck_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        code = getIntent().getStringExtra("code");
        name = getIntent().getStringExtra("title");
        if (code == null || name == null) {
            Toaster.show(this, Utils.Messages.FAILED_LOADING, new NullPointerException("code or name is null!"));
            onBackPressed();
            return;
        }

        final ViewPager pager = findViewById(R.id.cardcastDeck_pager);
        pager.setOffscreenPageLimit(3);
        TabLayout tabs = findViewById(R.id.cardcastDeck_tabs);
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
                InfoFragment.getInstance(this, code),
                CardsFragment.getInstance(this, false, code),
                CardsFragment.getInstance(this, true, code)));
        tabs.setupWithViewPager(pager);
    }
}
