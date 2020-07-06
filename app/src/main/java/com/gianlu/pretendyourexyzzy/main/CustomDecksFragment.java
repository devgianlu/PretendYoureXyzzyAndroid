package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.EditCustomDeckActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CustomDecksFragment extends FragmentWithDialog {
    private static final int RC_IMPORT_JSON = 2;
    private static final String TAG = CustomDecksFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private CustomDecksDatabase db;

    @NonNull
    public static CustomDecksFragment getInstance() {
        return new CustomDecksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_custom_decks, container, false);
        db = CustomDecksDatabase.get(requireContext());

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

        FloatingActionButton recoverDeck = layout.findViewById(R.id.customDecksFab_recover);
        recoverDeck.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5), new InputFilter.AllCaps()});

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.recoverCardcastDeck).setView(input)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.recover, (dialog, which) -> {
                        String code = input.getText().toString();
                        if (!code.matches("[A-Z0-9]{5}")) {
                            showToast(Toaster.build().message(R.string.invalidDeckCode));
                            return;
                        }

                        try {
                            recoverCardcastDeck(code);
                        } catch (LevelMismatchException ignored) {
                        }
                    });
            showDialog(builder);
            fab.collapse();
        });

        return layout;
    }

    private void recoverCardcastDeck(@NonNull String code) throws LevelMismatchException {
        showProgress(R.string.loading);
        Pyx.get().recoverCardcastDeck(code, requireContext(), new Pyx.OnRecoverResult() {
            @Override
            public void onDone(@NonNull File tmpFile) {
                dismissDialog();
                EditCustomDeckActivity.startActivityImport(requireContext(), false, tmpFile);
            }

            @Override
            public void notFound() {
                dismissDialog();
                showToast(Toaster.build().message(R.string.recoverDeckNotFound));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Cannot recover deck.", ex);
                showToast(Toaster.build().message(R.string.failedRecoveringCardcastDeck));
                dismissDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        List<CustomDeck> decks = db.getDecks();
        rmv.loadListData(new CustomDecksAdapter(decks), false);
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
                    EditCustomDeckActivity.startActivityImport(requireContext(), true, tmpFile);
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
            this.inflater = LayoutInflater.from(getContext());
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
            holder.itemView.setOnClickListener(view -> EditCustomDeckActivity.startActivityEdit(holder.itemView.getContext(), deck));
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
