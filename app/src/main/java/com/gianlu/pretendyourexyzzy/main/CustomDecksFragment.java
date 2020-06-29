package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksAdapter;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.FloatingCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.EditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.customdecks.ViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.overloaded.SyncUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

public class CustomDecksFragment extends FragmentWithDialog implements OverloadedSyncApi.SyncStatusListener, CustomDecksAdapter.Listener {
    private static final int RC_IMPORT_JSON = 2;
    private static final String TAG = CustomDecksFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private CustomDecksDatabase db;
    private TextView syncStatus;

    @NonNull
    public static CustomDecksFragment getInstance() {
        return new CustomDecksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_custom_decks, container, false);
        db = CustomDecksDatabase.get(requireContext());

        syncStatus = layout.findViewById(R.id.customDecks_sync);
        rmv = layout.findViewById(R.id.customDecks_list);
        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);

        FloatingActionsMenu fab = layout.findViewById(R.id.customDecks_fab);
        FloatingActionButton importDeck = layout.findViewById(R.id.customDecksFab_import);
        importDeck.setOnClickListener(v -> {
            startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), "Pick JSON file..."), RC_IMPORT_JSON);
            fab.collapse();
        });

        FloatingActionButton addDeck = layout.findViewById(R.id.customDecksFab_add);
        addDeck.setOnClickListener(v -> {
            EditCustomDeckActivity.startActivityNew(requireContext());
            fab.collapse();
        });

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        OverloadedSyncApi.get().addSyncListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        List<FloatingCustomDeck> decks = db.getAllDecks();
        rmv.loadListData(new CustomDecksAdapter(requireContext(), decks, this), false);
        if (decks.isEmpty()) rmv.showInfo(R.string.noCustomDecks_create);
        else rmv.showList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_IMPORT_JSON) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                    if (in == null) return;

                    File tmpFile = new File(requireContext().getCacheDir(), CommonUtils.randomString(6, "abcdefghijklmnopqrstuvwxyz"));
                    CommonUtils.copy(in, new FileOutputStream(tmpFile));
                    EditCustomDeckActivity.startActivityImport(requireContext(), tmpFile);
                } catch (IOException ex) {
                    Log.e(TAG, "Failed importing JSON file: " + data, ex);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void syncStatusUpdated(@NonNull OverloadedSyncApi.SyncProduct product, boolean isSyncing, boolean error) {
        if (syncStatus != null && product == OverloadedSyncApi.SyncProduct.CUSTOM_DECKS)
            SyncUtils.updateSyncText(syncStatus, product, isSyncing, error);
    }

    @Override
    public void onCustomDeckSelected(@NonNull FloatingCustomDeck deck) {
        if (deck instanceof CustomDeck)
            EditCustomDeckActivity.startActivityEdit(requireContext(), (CustomDeck) deck);
        else if (deck instanceof CustomDecksDatabase.StarredDeck && deck.owner != null)
            ViewCustomDeckActivity.startActivity(requireContext(), deck.owner, deck.name, ((CustomDecksDatabase.StarredDeck) deck).shareCode);
    }
}
