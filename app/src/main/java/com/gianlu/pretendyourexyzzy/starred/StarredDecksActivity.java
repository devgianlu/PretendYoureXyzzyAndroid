package com.gianlu.pretendyourexyzzy.starred;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.activities.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.adapters.StarredDecksAdapter;
import com.gianlu.pretendyourexyzzy.main.OngoingGameHelper;

public class StarredDecksActivity extends ActivityWithDialog implements StarredDecksAdapter.Listener {
    private RecyclerMessageView rmv;
    private StarredDecksManager starredDecks;

    public static void startActivity(Context context) {
        if (StarredDecksManager.get().hasAnyDeck())
            context.startActivity(new Intent(context, StarredDecksActivity.class));
        else
            Toaster.with(context).message(R.string.noStarredDecks).show();
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
        rmv = new RecyclerMessageView(this);
        setContentView(rmv);
        setTitle(R.string.starredCardcastDecks);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        starredDecks = StarredDecksManager.get();

        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.loadListData(new StarredDecksAdapter(this, starredDecks.getDecks(), this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        rmv.loadListData(new StarredDecksAdapter(this, starredDecks.getDecks(), this));
    }

    @Override
    public void onDeckSelected(@NonNull StarredDecksManager.StarredDeck deck) {
        CardcastDeckActivity.startActivity(this, deck.code, deck.name);
    }
}
