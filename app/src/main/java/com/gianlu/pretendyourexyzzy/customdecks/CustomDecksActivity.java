package com.gianlu.pretendyourexyzzy.customdecks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;

import java.util.List;

public class CustomDecksActivity extends ActivityWithDialog {
    private RecyclerMessageView rmv;
    private CustomDecksDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rmv = new RecyclerMessageView(this);
        setContentView(rmv);
        setTitle(R.string.customDecks);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        db = CustomDecksDatabase.get(this);

        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<CustomDeck> decks = db.getDecks();
        rmv.loadListData(new CustomDecksAdapter(decks), false);
        if (decks.isEmpty()) rmv.showInfo(R.string.noCustomDecks);
        else rmv.showList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_decks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.customDecks_add:
                EditCustomDeckActivity.startActivityNew(this);
                return true;
            case R.id.customDecks_import:
                // TODO: Import and show deck activity
                return true;
            default:
                return false;
        }
    }

    private class CustomDecksAdapter extends RecyclerView.Adapter<CustomDecksAdapter.ViewHolder> {
        private final List<CustomDeck> decks;
        private final LayoutInflater inflater;

        CustomDecksAdapter(@NonNull List<CustomDeck> decks) {
            this.decks = decks;
            this.inflater = LayoutInflater.from(CustomDecksActivity.this);
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            return decks.get(position).id;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CustomDeck deck = decks.get(position);
            holder.name.setText(deck.name);
            holder.watermark.setText(deck.watermark);
            holder.itemView.setOnClickListener(view -> {
                EditCustomDeckActivity.startActivityEdit(CustomDecksActivity.this, deck);
            });

            CommonUtils.setRecyclerViewTopMargin(holder);
        }

        @Override
        public int getItemCount() {
            return decks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView watermark;

            ViewHolder(ViewGroup parent) {
                super(inflater.inflate(R.layout.item_custom_deck, parent, false));

                name = itemView.findViewById(R.id.customDeckItem_name);
                watermark = itemView.findViewById(R.id.customDeckItem_watermark);
            }
        }
    }
}
