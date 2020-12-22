package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewEditCustomDeckInfoBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public class NewEditCustomDeckActivity extends AbsNewCustomDeckActivity {
    private static final String TAG = NewEditCustomDeckActivity.class.getSimpleName();

    @NotNull
    private static Intent baseStartIntent(@NotNull Context context, @NotNull Type type) {
        Intent intent = new Intent(context, NewEditCustomDeckActivity.class);
        intent.putExtra("type", type);
        return intent;
    }

    @NotNull
    public static Intent activityEditIntent(@NotNull Context context, @NotNull CustomDeck deck) {
        Intent intent = baseStartIntent(context, Type.EDIT);
        intent.putExtra("deckId", deck.id);
        return intent;
    }

    @NotNull
    public static Intent activityNewIntent(@NotNull Context context) {
        return baseStartIntent(context, Type.NEW);
    }

    @NotNull
    public static Intent activityImportRecoverIntent(@NotNull Context context, @NotNull File tmpFile) {
        Intent intent = baseStartIntent(context, Type.IMPORT);
        intent.putExtra("tmpFile", tmpFile);
        return intent;
    }

    @Override
    protected void onInflateMenu(@NotNull MenuInflater inflater, @NotNull Menu menu) {
        inflater.inflate(R.menu.new_edit_custom_deck, menu);

        if (getDeckId() == null) {
            menu.removeItem(R.id.editCustomDeck_delete);
            menu.removeItem(R.id.editCustomDeck_export);
        }
    }

    @Override
    protected void onSaved(@NotNull Bundle bundle) {
        setMenuIconVisible(true);
        setPageChangeAllowed(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int deckId = getIntent().getIntExtra("deckId", -1);
        setMenuIconVisible(deckId != -1);
        setPageChangeAllowed(deckId != -1);

        // Handle intent-filter
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri == null) uri = getIntent().getData();
            if (uri == null) {
                finishAfterTransition();
                return;
            }

            setBottomButtonMode(Mode.SAVE);

            try (InputStream in = getContentResolver().openInputStream(uri)) {
                importStream(in);
            } catch (JSONException | IOException ex) {
                Log.e(TAG, "Failed importing deck from uri.", ex);
                finishAfterTransition();
                return;
            }
        }

        Type type = (Type) getIntent().getSerializableExtra("type");
        if (type == null) return;

        switch (type) {
            case NEW:
                setBottomButtonMode(Mode.CONTINUE);
                loaded(InfoFragment.empty(), BlacksFragment.empty(), WhitesFragment.empty());
                break;
            case EDIT:
                setBottomButtonMode(Mode.SAVE);
                loaded(InfoFragment.get(deckId), BlacksFragment.get(deckId), WhitesFragment.get(deckId));
                break;
            case IMPORT:
                setBottomButtonMode(Mode.SAVE);

                File tmpFile = (File) getIntent().getSerializableExtra("tmpFile");
                if (tmpFile == null) break;

                try (FileInputStream in = new FileInputStream(tmpFile)) {
                    importStream(in);
                } catch (JSONException | IOException ex) {
                    Log.e(TAG, "Failed importing deck from file.", ex);
                    finishAfterTransition();
                } finally {
                    tmpFile.deleteOnExit();
                }
                break;
        }
    }

    /**
     * Imports the deck reading the JSON from the input stream. PLEASE CLOSE IT.
     *
     * @param in The input stream
     */
    private void importStream(@NonNull InputStream in) throws IOException, JSONException {
        JSONObject obj = new JSONObject(CommonUtils.readEntirely(in));

        CardsFragment whitesFragment, blacksFragment;
        loaded(InfoFragment.json(obj), blacksFragment = BlacksFragment.empty(), whitesFragment = WhitesFragment.empty());
        if (save()) { // Will not import cards if can't save (meh...)
            blacksFragment.importCards(this, obj.optJSONArray("calls"));
            whitesFragment.importCards(this, obj.optJSONArray("responses"));
        }

        ThisApplication.sendAnalytics(Utils.ACTION_IMPORTED_CUSTOM_DECK);
    }

    @Override
    protected boolean onMenuItemSelected(@NotNull MenuItem item) {
        if (item.getItemId() == R.id.editCustomDeck_export) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_EXPORTED_CUSTOM_DECK);
            exportCustomDeckJson();
            return true;
        } else if (item.getItemId() == R.id.editCustomDeck_delete) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.delete).setMessage(getString(R.string.deleteDeckConfirmation, getName()))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Integer deckId = getDeckId();
                        if (deckId == null) return;

                        ThisApplication.sendAnalytics(Utils.ACTION_DELETED_CUSTOM_DECK);
                        CustomDecksDatabase.get(this).deleteDeckAndCards(deckId, true);
                        onBackPressed();
                    }).setNegativeButton(android.R.string.no, null);

            showDialog(builder);
            return true;
        } else {
            return super.onMenuItemSelected(item);
        }
    }

    private void exportCustomDeckJson() {
        Integer deckId = getDeckId();
        if (deckId == null)
            return;

        CustomDecksDatabase db = CustomDecksDatabase.get(this);
        CustomDecksDatabase.CustomDeck deck = db.getDeck(deckId);
        if (deck == null)
            return;

        try {
            JSONObject obj = deck.craftPyxJson(db);

            File parent = new File(getCacheDir(), "exportedDecks");
            if (!parent.exists() && !parent.mkdir()) {
                Log.e(TAG, "Failed creating exported decks directory: " + parent);
                return;
            }

            String fileName = getName();
            if (fileName.isEmpty()) fileName = String.valueOf(deckId);

            File file = new File(parent, fileName + ".deck.json");
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(obj.toString().getBytes());
            }

            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName(), file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share custom deck..."));
        } catch (JSONException | IOException | IllegalArgumentException ex) {
            Log.e(TAG, "Failed exporting custom deck!", ex);
        }
    }

    private enum Type {
        NEW, EDIT, IMPORT
    }

    public static class InfoFragment extends FragmentWithDialog implements SavableFragment {
        private static final Pattern VALID_WATERMARK_PATTERN = Pattern.compile("[A-Z0-9]{5}");
        private static final int MIN_DECK_NAME_LENGTH = 5;
        private static final int MAX_DECK_NAME_LENGTH = 32;
        private static final int MAX_DECK_DESC_LENGTH = 256;
        private FragmentNewEditCustomDeckInfoBinding binding;
        private CustomDeck deck;
        private CollaboratorsAdapter collaboratorsAdapter;

        @NotNull
        public static InfoFragment empty() {
            InfoFragment fragment = new InfoFragment();
            fragment.setArguments(new Bundle());
            return fragment;
        }

        @NotNull
        public static InfoFragment json(@NotNull JSONObject obj) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putInt("deckId", -1);
            args.putBoolean("import", true);
            args.putString("name", obj.optString("name"));
            args.putString("watermark", obj.optString("watermark"));
            args.putString("desc", obj.optString("description"));
            fragment.setArguments(args);
            return fragment;
        }

        @NotNull
        public static InfoFragment get(int deckId) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putBoolean("import", false);
            args.putInt("deckId", deckId);
            fragment.setArguments(args);
            return fragment;
        }

        private void setCollaboratorsStatus(boolean loading, boolean notSignedIn, boolean notSynced, boolean empty, boolean error) {
            if (loading) {
                binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.GONE);
                binding.editCustomDeckAddCollaborator.setVisibility(View.GONE);
                binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsNotSynced.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsError.setVisibility(View.GONE);

                binding.editCustomDeckCollaboratorsLoading.setVisibility(View.VISIBLE);
                binding.editCustomDeckCollaboratorsLoading.showShimmer(true);
            } else if (notSignedIn) {
                binding.editCustomDeckCollaboratorsLoading.setVisibility(View.GONE);
                binding.editCustomDeckAddCollaborator.setVisibility(View.GONE);
                binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsNotSynced.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsError.setVisibility(View.GONE);

                binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.VISIBLE);
            } else if (notSynced) {
                binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsLoading.setVisibility(View.GONE);
                binding.editCustomDeckAddCollaborator.setVisibility(View.GONE);
                binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsError.setVisibility(View.GONE);

                binding.editCustomDeckCollaboratorsNotSynced.setVisibility(View.VISIBLE);
            } else if (empty) {
                binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsLoading.setVisibility(View.GONE);
                binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsNotSynced.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsError.setVisibility(View.GONE);

                binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.VISIBLE);
                binding.editCustomDeckAddCollaborator.setVisibility(View.VISIBLE);
            } else if (error) {
                binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsLoading.setVisibility(View.GONE);
                binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsNotSynced.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);

                binding.editCustomDeckAddCollaborator.setVisibility(View.VISIBLE);
                binding.editCustomDeckCollaboratorsError.setVisibility(View.VISIBLE);
            } else {
                binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsLoading.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsNotSynced.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);
                binding.editCustomDeckCollaboratorsError.setVisibility(View.GONE);

                binding.editCustomDeckAddCollaborator.setVisibility(View.VISIBLE);
                binding.editCustomDeckCollaborators.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean save(@NotNull Bundle bundle, @NotNull Callback callback) {
            boolean result;
            if (requireArguments().getBoolean("import", false) && binding == null)
                result = save(requireArguments().getString("name", ""), requireArguments().getString("watermark", ""), requireArguments().getString("desc", ""), callback.getDb());
            else
                result = save(CommonUtils.getText(binding.editCustomDeckInfoName), CommonUtils.getText(binding.editCustomDeckInfoWatermark), CommonUtils.getText(binding.editCustomDeckInfoDesc), callback.getDb());

            if (result) {
                bundle.putString("name", deck.name);
                bundle.putString("watermark", deck.watermark);
                bundle.putInt("deckId", deck.id);
            } else {
                if (CommonUtils.getText(binding.editCustomDeckInfoName).isEmpty() || CommonUtils.getText(binding.editCustomDeckInfoWatermark).isEmpty())
                    showToast(Toaster.build().message(R.string.completeDeckInfoFirst));
            }

            return result;
        }

        private boolean save(@NonNull String name, @NonNull String watermark, @NonNull String description, CustomDecksDatabase db) {
            if (db == null) return false;

            name = name.trim();
            if (name.length() < MIN_DECK_NAME_LENGTH || name.length() > MAX_DECK_NAME_LENGTH)
                return false;

            if (!VALID_WATERMARK_PATTERN.matcher(watermark).matches())
                return false;

            description = description.trim();
            if (description.length() > MAX_DECK_DESC_LENGTH)
                return false;

            if (deck == null) {
                ThisApplication.sendAnalytics(Utils.ACTION_CREATED_CUSTOM_DECK);

                deck = db.putDeckInfo(name, watermark, description);
                return deck != null;
            } else {
                db.updateDeckInfo(deck.id, name, watermark, description);
                return true;
            }
        }

        private void setupCollaborators() {
            setCollaboratorsStatus(true, false, false, false, false);

            OverloadedUtils.waitReady()
                    .addOnSuccessListener(signedIn -> {
                        if (signedIn) {
                            if (deck == null || deck.remoteId == null) {
                                setCollaboratorsStatus(false, false, true, false, false);
                            } else {
                                OverloadedSyncApi.get().getCollaborators(deck.remoteId)
                                        .addOnSuccessListener(list -> binding.editCustomDeckCollaborators.setAdapter(collaboratorsAdapter = new CollaboratorsAdapter(list)))
                                        .addOnFailureListener(ex -> {
                                            Log.e(TAG, "Failed getting collaborators.", ex);
                                            setCollaboratorsStatus(false, false, true, false, true);
                                        });
                            }
                        } else {
                            setCollaboratorsStatus(false, true, false, false, false);
                        }
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed waiting ready.", ex);
                        setCollaboratorsStatus(false, false, true, false, true);
                    });
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentNewEditCustomDeckInfoBinding.inflate(inflater, container, false);
            binding.editCustomDeckCollaborators.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
            binding.editCustomDeckAddCollaborator.setOnClickListener(v -> showAddCollaboratorDialog());
            Utils.generateUsernamePlaceholders(requireContext(), binding.editCustomDeckCollaboratorsLoadingChild, 16, 16, 8);

            CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());

            CommonUtils.getEditText(binding.editCustomDeckInfoName).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s.toString().trim();
                    if (str.length() < MIN_DECK_NAME_LENGTH || str.length() > MAX_DECK_NAME_LENGTH) {
                        binding.editCustomDeckInfoName.setErrorEnabled(true);
                        binding.editCustomDeckInfoName.setError(getString(R.string.invalidDeckName));
                    } else if ((deck == null || !deck.name.equals(str)) && !db.isNameUnique(str)
                            && (!requireArguments().getBoolean("import", false) || !requireArguments().getString("name", "").equals(str))) {
                        binding.editCustomDeckInfoName.setErrorEnabled(true);
                        binding.editCustomDeckInfoName.setError(getString(R.string.customDeckNameNotUnique));
                    } else {
                        binding.editCustomDeckInfoName.setErrorEnabled(false);
                    }
                }
            });
            CommonUtils.getEditText(binding.editCustomDeckInfoWatermark).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s.toString();
                    if (VALID_WATERMARK_PATTERN.matcher(str).matches()) {
                        binding.editCustomDeckInfoWatermark.setErrorEnabled(false);
                    } else {
                        binding.editCustomDeckInfoWatermark.setErrorEnabled(true);
                        binding.editCustomDeckInfoWatermark.setError(getString(R.string.invalidWatermark));
                    }
                }
            });
            CommonUtils.getEditText(binding.editCustomDeckInfoWatermark).setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(5)});
            CommonUtils.getEditText(binding.editCustomDeckInfoDesc).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s.toString().trim();
                    if (str.length() > MAX_DECK_DESC_LENGTH) {
                        binding.editCustomDeckInfoDesc.setErrorEnabled(true);
                        binding.editCustomDeckInfoDesc.setError(getString(R.string.invalidDeckDesc));
                    } else {
                        binding.editCustomDeckInfoDesc.setErrorEnabled(false);
                    }
                }
            });

            if (requireArguments().getBoolean("import", false)) {
                CommonUtils.setText(binding.editCustomDeckInfoName, requireArguments().getString("name"));
                CommonUtils.setText(binding.editCustomDeckInfoWatermark, requireArguments().getString("watermark"));
                CommonUtils.setText(binding.editCustomDeckInfoDesc, requireArguments().getString("desc"));
            } else {
                int deckId = requireArguments().getInt("deckId", -1);
                if (deckId == -1) deck = null;
                else deck = db.getDeck(deckId);

                if (deck != null) {
                    CommonUtils.setText(binding.editCustomDeckInfoName, deck.name);
                    CommonUtils.setText(binding.editCustomDeckInfoWatermark, deck.watermark);
                    CommonUtils.setText(binding.editCustomDeckInfoDesc, deck.description);
                }
            }

            setupCollaborators();

            return binding.getRoot();
        }

        private void showAddCollaboratorDialog() {
            showProgress(R.string.loading);
            OverloadedApi.get().friendsStatus()
                    .addOnSuccessListener(result -> {
                        dismissDialog();
                        if (getContext() == null)
                            return;

                        List<String> friends = new ArrayList<>(result.size());
                        for (Map.Entry<String, FriendStatus> entry : result.entrySet())
                            if (entry.getValue().mutual) friends.add(entry.getKey());

                        if (collaboratorsAdapter != null)
                            friends.removeAll(collaboratorsAdapter.list);

                        if (friends.isEmpty()) {
                            showToast(Toaster.build().message(R.string.noCollaboratorsToAdd));
                            return;
                        }

                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                        builder.setTitle(R.string.addCollaborator)
                                .setNeutralButton(R.string.cancel, null)
                                .setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, friends), (dialog, which) -> {
                                    dialog.dismiss();

                                    if (deck == null || deck.remoteId == null)
                                        return;

                                    showProgress(R.string.loading);
                                    String friend = friends.get(which);
                                    OverloadedSyncApi.get().addCollaborator(deck.remoteId, friend)
                                            .addOnSuccessListener(collaborators -> {
                                                dismissDialog();
                                                if (collaboratorsAdapter != null)
                                                    collaboratorsAdapter.setCollaborators(collaborators);
                                            })
                                            .addOnFailureListener(ex -> {
                                                Log.e(TAG, "Failed adding collaborator: " + friend, ex);
                                                showToast(Toaster.build().message(R.string.failedAddingCollaborator));
                                                dismissDialog();
                                            });
                                });

                        showDialog(builder);
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "Failed getting friends list.", ex);

                        dismissDialog();
                        showToast(Toaster.build().message(R.string.failedLoading));
                    });
        }

        private class CollaboratorsAdapter extends RecyclerView.Adapter<CollaboratorsAdapter.ViewHolder> {
            private final List<String> list;

            CollaboratorsAdapter(@NotNull List<String> list) {
                this.list = list;
                countUpdated(list.size());
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolder(parent);
            }

            private void setCollaborators(@NotNull List<String> newList) {
                list.clear();
                list.addAll(newList);
                notifyDataSetChanged();

                countUpdated(list.size());
            }

            private void countUpdated(int count) {
                setCollaboratorsStatus(false, false, false, count == 0, false);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                String username = list.get(position);
                holder.text.setText(username);

                PopupMenu popup = new PopupMenu(requireContext(), holder.text);
                popup.inflate(R.menu.item_collaborator);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.collaboratorItemMenu_remove) {
                        if (deck == null || deck.remoteId == null)
                            return false;

                        showProgress(R.string.loading);
                        OverloadedSyncApi.get().removeCollaborator(deck.remoteId, username)
                                .addOnSuccessListener(result -> {
                                    dismissDialog();
                                    setCollaborators(result);
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed removing collaborator: " + username, ex);
                                    dismissDialog();

                                    showToast(Toaster.build().message(R.string.failedRemovingCollaborator));
                                });
                        return true;
                    }

                    return false;
                });

                holder.text.setOnClickListener(v -> CommonUtils.showPopupOffset(popup, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()), 0));
            }

            @Override
            public int getItemCount() {
                return list.size();
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                final TextView text;

                ViewHolder(@NonNull ViewGroup parent) {
                    super(getLayoutInflater().inflate(R.layout.item_collaborator, parent, false));
                    text = (TextView) itemView;
                }
            }
        }
    }

    private static abstract class CardsFragment extends AbsNewCardsFragment implements SavableFragment {
        private Integer deckId;
        private CustomDecksDatabase db;
        private String watermark;

        @Override
        protected final boolean canEditCards() {
            return true;
        }

        @NotNull
        @Override
        protected final String getWatermark() {
            return watermark == null ? "" : watermark;
        }

        @Override
        public final boolean save(@NotNull Bundle bundle, @NotNull Callback callback) {
            if (deckId == null) {
                deckId = bundle.getInt("deckId", -1);
                if (deckId == -1) return false;

                watermark = bundle.getString("watermark");

                setHandler(new CustomDecksHandler(callback.getDb(), deckId));
            }

            return super.save(bundle, callback);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            db = CustomDecksDatabase.get(requireContext());

            deckId = requireArguments().getInt("deckId", -1);
            if (deckId == -1) deckId = null;

            if (deckId != null) {
                CustomDeck deck = db.getDeck(deckId);
                if (deck != null) watermark = deck.watermark;

                setHandler(new CustomDecksHandler(db, deckId));
            }
        }

        @Override
        public boolean isBlack() {
            return this instanceof BlacksFragment;
        }

        @NotNull
        @Override
        protected final List<? extends BaseCard> getCards(@NotNull Context context) {
            if (deckId == null) return new ArrayList<>();
            else return isBlack() ? db.getBlackCards(deckId) : db.getWhiteCards(deckId);
        }

        /**
         * Import cards from JSON. Caller should make sure which type (black, white) the cards are and which fragment it's calling.
         *
         * @param context The caller {@link Context}
         * @param array   The array containing the cards or {@code null}
         */
        public void importCards(@NonNull Context context, @Nullable JSONArray array) {
            if (array == null) return;

            if (db == null)
                db = CustomDecksDatabase.get(context);

            String[][] texts = new String[array.length()][];
            boolean[] blacks = new boolean[array.length()];
            for (int i = 0; i < array.length(); i++) {
                try {
                    String[] text = CommonUtils.toStringArray(array.getJSONObject(i).getJSONArray("text"));
                    if (isBlack() && text.length == 1)
                        text = new String[]{text[0] + " ", ""};

                    texts[i] = text;
                    blacks[i] = isBlack();
                } catch (JSONException ex) {
                    Log.w(TAG, "Failed importing card at " + i, ex);
                }
            }

            addCards(blacks, texts);
        }
    }

    public static class BlacksFragment extends CardsFragment {

        @NotNull
        public static BlacksFragment empty() {
            BlacksFragment fragment = new BlacksFragment();
            fragment.setArguments(new Bundle());
            return fragment;
        }

        @NotNull
        public static BlacksFragment get(int deckId) {
            BlacksFragment fragment = new BlacksFragment();
            Bundle args = new Bundle();
            args.putInt("deckId", deckId);
            fragment.setArguments(args);
            return fragment;
        }
    }

    public static class WhitesFragment extends CardsFragment {

        @NotNull
        public static WhitesFragment empty() {
            WhitesFragment fragment = new WhitesFragment();
            fragment.setArguments(new Bundle());
            return fragment;
        }

        @NotNull
        public static WhitesFragment get(int deckId) {
            WhitesFragment fragment = new WhitesFragment();
            Bundle args = new Bundle();
            args.putInt("deckId", deckId);
            fragment.setArguments(args);
            return fragment;
        }
    }
}
