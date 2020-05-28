package com.gianlu.pretendyourexyzzy.customdecks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CustomDecksActivity extends ActivityWithDialog {
    private static final int RC_IMPORT_JSON = 2;
    private static final String TAG = CustomDecksActivity.class.getSimpleName();
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
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), "Pick JSON file..."), RC_IMPORT_JSON);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_IMPORT_JSON) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    if (in == null) return;

                    File tmpFile = new File(getCacheDir(), CommonUtils.randomString(6, "abcdefghijklmnopqrstuvwxyz"));
                    CommonUtils.copy(in, new FileOutputStream(tmpFile));
                    EditCustomDeckActivity.startActivityImport(this, tmpFile);
                } catch (IOException ex) {
                    Log.e(TAG, "Failed importing JSON file: " + data, ex);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
