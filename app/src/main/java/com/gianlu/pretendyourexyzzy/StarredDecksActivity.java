package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.StarredDecksAdapter;
import com.gianlu.pretendyourexyzzy.Cards.StarredDecksManager;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameHelper;

public class StarredDecksActivity extends ActivityWithDialog implements StarredDecksAdapter.Listener {
    private RecyclerViewLayout layout;

    public static void startActivity(Context context) {
        if (StarredDecksManager.hasAnyDeck(context)) {
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
        OngoingGameHelper.Listener listener = OngoingGameHelper.get();
        menu.findItem(R.id.starredDecks_add).setVisible(listener != null && listener.canModifyCardcastDecks());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.starredDecks_add:
                OngoingGameHelper.Listener listener = OngoingGameHelper.get();
                if (listener != null) listener.addCardcastStarredDecks();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new RecyclerViewLayout(this);
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
    protected void onResume() {
        super.onResume();

        layout.loadListData(new StarredDecksAdapter(this, StarredDecksManager.loadDecks(this), this));
    }

    @Override
    public void onDeckSelected(StarredDecksManager.StarredDeck deck) {
        CardcastDeckActivity.startActivity(this, deck.code, deck.name);
    }
}
