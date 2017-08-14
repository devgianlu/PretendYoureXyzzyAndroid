package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.gianlu.cardcastapi.Models.Deck;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.PagerAdapter;
import com.gianlu.pretendyourexyzzy.CardcastDeck.CardsFragment;
import com.gianlu.pretendyourexyzzy.CardcastDeck.InfoFragment;

public class CardcastDeckActivity extends AppCompatActivity {

    public static void startActivity(Context context, Deck deck) {
        startActivity(context, deck.code, deck.name);
    }

    public static void startActivity(Context context, String code, String name) {
        context.startActivity(new Intent(context, CardcastDeckActivity.class)
                .putExtra("code", code)
                .putExtra("title", name));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardcast_deck);
        setTitle(getIntent().getStringExtra("title"));

        setSupportActionBar((Toolbar) findViewById(R.id.cardcastDeck_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        String code = getIntent().getStringExtra("code");
        if (code == null) {
            Toaster.show(this, Utils.Messages.FAILED_LOADING, new NullPointerException("code is null!"));
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
