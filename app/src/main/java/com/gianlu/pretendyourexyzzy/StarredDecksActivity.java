package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.StarredDecksAdapter;
import com.gianlu.pretendyourexyzzy.Cards.StarredDecksManager;

public class StarredDecksActivity extends AppCompatActivity implements StarredDecksAdapter.IAdapter {
    private static CardcastDeckActivity.IOngoingGame handler;

    public static void startActivity(Context context, CardcastDeckActivity.IOngoingGame handler) {
        if (StarredDecksManager.hasAnyDeck(context)) {
            StarredDecksActivity.handler = handler;
            context.startActivity(new Intent(context, StarredDecksActivity.class));
        } else {
            Toaster.show(context, Utils.Messages.NO_STARRED_DECKS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.starred_decks, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.starredDecks_add).setVisible(handler != null && handler.canAddCardcastDeck());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.starredDecks_add:
                handler.addCardcastStarredDecks();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecyclerViewLayout layout = new RecyclerViewLayout(this);
        setContentView(layout);
        setTitle(R.string.starredDecks);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        layout.disableSwipeRefresh();
        layout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary_background));
        layout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        layout.loadListData(new StarredDecksAdapter(this, StarredDecksManager.loadDecks(this), this));
    }

    @Override
    public void onDeckSelected(StarredDecksManager.StarredDeck deck) {
        CardcastDeckActivity.startActivity(this, deck.code, deck.name, handler);
    }
}
