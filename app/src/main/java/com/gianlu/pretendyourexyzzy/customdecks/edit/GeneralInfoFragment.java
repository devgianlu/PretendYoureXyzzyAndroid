package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;
import xyz.gianlu.pyxoverloaded.callback.FriendsStatusCallback;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public final class GeneralInfoFragment extends FragmentWithDialog {
    private static final Pattern VALID_WATERMARK_PATTERN = Pattern.compile("[A-Z0-9]{5}");
    private static final int MIN_DECK_NAME_LENGTH = 5;
    private static final int MAX_DECK_NAME_LENGTH = 32;
    private static final int MAX_DECK_DESC_LENGTH = 256;
    private static final String TAG = GeneralInfoFragment.class.getSimpleName();
    private TextInputLayout name;
    private TextInputLayout watermark;
    private TextInputLayout desc;
    private LinearLayout collaborators;
    private MessageView collaboratorsMessage;
    private ProgressBar collaboratorsLoading;
    private ImageButton addCollaborator;
    private CustomDecksDatabase db;
    private CustomDeck deck;
    private String importName;
    private String importDesc;
    private String importWatermark;

    @NonNull
    public static GeneralInfoFragment get(@NonNull Context context, @Nullable Integer id) {
        GeneralInfoFragment fragment = new GeneralInfoFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        if (id != null) args.putInt("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public String getName() {
        return importName != null ? importName : (name == null ? "" : CommonUtils.getText(name));
    }

    @NotNull
    public String getWatermark() {
        return importWatermark != null ? importWatermark : (watermark == null ? "" : CommonUtils.getText(watermark));
    }

    @Nullable
    public Integer getDeckId() {
        return deck == null ? null : deck.id;
    }

    public boolean isSaved() {
        return deck != null;
    }

    private boolean save(@NonNull Context context, @NonNull String name, @NonNull String watermark, @NonNull String description) {
        name = name.trim();
        if (name.length() < MIN_DECK_NAME_LENGTH || name.length() > MAX_DECK_NAME_LENGTH)
            return false;

        if (!VALID_WATERMARK_PATTERN.matcher(watermark).matches())
            return false;

        description = description.trim();
        if (description.length() > MAX_DECK_DESC_LENGTH)
            return false;

        CustomDecksDatabase db = CustomDecksDatabase.get(context);
        if (deck == null) {
            ThisApplication.sendAnalytics(Utils.ACTION_CREATED_CUSTOM_DECK);

            deck = db.putDeckInfo(name, watermark, description);
            return deck != null;
        } else {
            db.updateDeckInfo(deck.id, name, watermark, description);
            return true;
        }
    }

    public boolean save(@NonNull Context context) {
        if (importName != null && importDesc != null && importWatermark != null) {
            boolean result = save(context, importName, importWatermark, importDesc);
            if (result) importName = importDesc = importWatermark = null;
            return result;
        }

        if (name == null || watermark == null || desc == null)
            return false;

        return save(context, CommonUtils.getText(name), CommonUtils.getText(watermark), CommonUtils.getText(desc));
    }

    @NotNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView layout = (ScrollView) inflater.inflate(R.layout.fragment_edit_custom_deck_info, container, false);
        name = layout.findViewById(R.id.editCustomDeckInfo_name);
        CommonUtils.getEditText(name).addTextChangedListener(new TextWatcher() {
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
                    name.setErrorEnabled(true);
                    name.setError(getString(R.string.invalidDeckName));
                } else if ((deck == null || !deck.name.equals(str)) && db != null && !db.isNameUnique(str)) {
                    name.setErrorEnabled(true);
                    name.setError(getString(R.string.customDeckNameNotUnique));
                } else {
                    name.setErrorEnabled(false);
                }
            }
        });
        watermark = layout.findViewById(R.id.editCustomDeckInfo_watermark);
        CommonUtils.getEditText(watermark).addTextChangedListener(new TextWatcher() {
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
                    watermark.setErrorEnabled(false);
                } else {
                    watermark.setErrorEnabled(true);
                    watermark.setError(getString(R.string.invalidWatermark));
                }
            }
        });
        desc = layout.findViewById(R.id.editCustomDeckInfo_desc);
        CommonUtils.getEditText(desc).addTextChangedListener(new TextWatcher() {
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
                    desc.setErrorEnabled(true);
                    desc.setError(getString(R.string.invalidDeckDesc));
                } else {
                    desc.setErrorEnabled(false);
                }
            }
        });

        collaborators = layout.findViewById(R.id.editCustomDeck_collaborators);
        collaboratorsMessage = layout.findViewById(R.id.editCustomDeck_collaboratorsMessage);
        collaboratorsLoading = layout.findViewById(R.id.editCustomDeck_collaboratorsLoading);
        addCollaborator = layout.findViewById(R.id.editCustomDeck_addCollaborator);

        db = CustomDecksDatabase.get(requireContext());

        if (deck == null) {
            int id = requireArguments().getInt("id", -1);
            if (id == -1) deck = null;
            else deck = db.getDeck(id);
        }

        if (deck != null) {
            CommonUtils.setText(name, deck.name);
            CommonUtils.setText(watermark, deck.watermark);
            CommonUtils.setText(desc, deck.description);
        } else {
            if (importName != null) CommonUtils.setText(name, importName);
            if (importWatermark != null) CommonUtils.setText(watermark, importWatermark);
            if (importDesc != null) CommonUtils.setText(desc, importDesc);
            importName = importDesc = importWatermark = null;
        }

        //region Collaborators
        if (OverloadedUtils.isSignedIn()) {
            if (deck == null || deck.remoteId == null) {
                addCollaborator.setEnabled(false);
                collaborators.setVisibility(View.GONE);
                collaboratorsLoading.setVisibility(View.GONE);
                collaboratorsMessage.setVisibility(View.VISIBLE);
                collaboratorsMessage.info(R.string.collaboratorsDeckNotSynced);
            } else {
                long deckRemoteId = deck.remoteId;

                addCollaborator.setEnabled(false);
                collaborators.setVisibility(View.GONE);
                collaboratorsMessage.setVisibility(View.GONE);
                collaboratorsLoading.setVisibility(View.VISIBLE);
                OverloadedSyncApi.get().getCollaborators(deckRemoteId, getActivity(), new GeneralCallback<List<String>>() {
                    @Override
                    public void onResult(@NonNull List<String> result) {
                        setCollaborators(deckRemoteId, result);

                        addCollaborator.setEnabled(true);
                        addCollaborator.setOnClickListener(v -> showAddCollaboratorDialog(deckRemoteId, result));
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        Log.e(TAG, "Failed getting collaborators.", ex);

                        collaborators.setVisibility(View.GONE);
                        collaboratorsLoading.setVisibility(View.GONE);
                        collaboratorsMessage.setVisibility(View.VISIBLE);
                        collaboratorsMessage.error(R.string.failedLoading);
                    }
                });
            }
        } else {
            addCollaborator.setEnabled(false);
            collaborators.setVisibility(View.GONE);
            collaboratorsLoading.setVisibility(View.GONE);
            collaboratorsMessage.setVisibility(View.VISIBLE);
            collaboratorsMessage.info(R.string.featureOverloadedOnly);
        }
        //endregion

        return layout;
    }

    public void set(@NonNull String name, @NonNull String watermark, @NonNull String description) {
        if (this.name != null && this.watermark != null && this.desc != null) {
            CommonUtils.setText(this.name, name);
            CommonUtils.setText(this.watermark, watermark);
            CommonUtils.setText(this.desc, description);
        } else {
            importName = name;
            importDesc = description;
            importWatermark = watermark;
        }
    }

    //region Collaborators
    private void showAddCollaboratorDialog(long deckRemoteId, @NonNull List<String> alreadyCollaborators) {
        showProgress(R.string.loading);
        OverloadedApi.get().friendsStatus(getActivity(), new FriendsStatusCallback() {
            @Override
            public void onFriendsStatus(@NotNull Map<String, FriendStatus> result) {
                dismissDialog();
                if (getContext() == null)
                    return;

                List<String> friends = new ArrayList<>(result.size());
                for (Map.Entry<String, FriendStatus> entry : result.entrySet())
                    if (!entry.getValue().mutual) friends.remove(entry.getKey());

                friends.removeAll(alreadyCollaborators);

                if (friends.isEmpty()) {
                    showToast(Toaster.build().message(R.string.noCollaboratorsToAdd));
                    return;
                }

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setTitle(R.string.addCollaborator)
                        .setNeutralButton(R.string.cancel, null)
                        .setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, friends), (dialog, which) -> {
                            dialog.dismiss();

                            showProgress(R.string.loading);
                            String friend = friends.get(which);
                            OverloadedSyncApi.get().addCollaborator(deckRemoteId, friend, getActivity(), new GeneralCallback<List<String>>() {
                                @Override
                                public void onResult(@NonNull List<String> collaborators) {
                                    dismissDialog();
                                    setCollaborators(deckRemoteId, collaborators);
                                }

                                @Override
                                public void onFailed(@NonNull Exception ex) {
                                    Log.e(TAG, "Failed adding collaborator: " + friend, ex);
                                    showToast(Toaster.build().message(R.string.failedAddingCollaborator));
                                    dismissDialog();
                                }
                            });
                        });

                showDialog(builder);
            }

            @Override
            public void onFailed(@NotNull Exception ex) {
                Log.e(TAG, "Failed getting friends list.", ex);

                dismissDialog();
                showToast(Toaster.build().message(R.string.failedLoading));
            }
        });
    }

    private void setCollaborators(long remoteDeckId, @NonNull List<String> list) {
        collaborators.removeAllViews();

        collaboratorsLoading.setVisibility(View.GONE);
        if (list.isEmpty()) {
            collaborators.setVisibility(View.GONE);
            collaboratorsMessage.setVisibility(View.VISIBLE);
            collaboratorsMessage.info(R.string.noCollaborators);
            return;
        }

        collaboratorsMessage.setVisibility(View.GONE);
        collaborators.setVisibility(View.VISIBLE);

        for (String username : list) {
            TextView text = (TextView) getLayoutInflater().inflate(R.layout.item_collaborator, collaborators, false);
            text.setText(username);
            collaborators.addView(text);

            PopupMenu popup = new PopupMenu(collaborators.getContext(), text);
            popup.inflate(R.menu.item_collaborator);
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.collaboratorItemMenu_remove) {
                    showProgress(R.string.loading);
                    OverloadedSyncApi.get().removeCollaborator(remoteDeckId, username, getActivity(), new GeneralCallback<List<String>>() {
                        @Override
                        public void onResult(@NonNull List<String> result) {
                            dismissDialog();
                            setCollaborators(remoteDeckId, result);
                        }

                        @Override
                        public void onFailed(@NonNull Exception ex) {
                            Log.e(TAG, "Failed removing collaborator: " + username, ex);
                            dismissDialog();

                            showToast(Toaster.build().message(R.string.failedRemovingCollaborator));
                        }
                    });
                    return true;
                }

                return false;
            });

            text.setOnClickListener(v -> CommonUtils.showPopupOffset(popup, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()), 0));
        }
    }
    //endregion
}
