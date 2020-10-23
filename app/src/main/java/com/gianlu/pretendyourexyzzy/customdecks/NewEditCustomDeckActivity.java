package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewEditCustomDeckInfoBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import xyz.gianlu.pyxoverloaded.OverloadedSyncApi;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Type type = (Type) getIntent().getSerializableExtra("type");
        if (type == null) return;

        int deckId = getIntent().getIntExtra("deckId", -1);

        // TODO: Export deck
        // TODO: Delete deck

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
                // TODO: Import deck
                break;
        }
    }

    private enum Type {
        NEW, EDIT, IMPORT
    }

    public static class InfoFragment extends FragmentWithDialog {
        private static final Pattern VALID_WATERMARK_PATTERN = Pattern.compile("[A-Z0-9]{5}");
        private static final int MIN_DECK_NAME_LENGTH = 5;
        private static final int MAX_DECK_NAME_LENGTH = 32;
        private static final int MAX_DECK_DESC_LENGTH = 256;
        private FragmentNewEditCustomDeckInfoBinding binding;
        private CustomDeck deck;

        @NotNull
        public static InfoFragment empty() {
            InfoFragment fragment = new InfoFragment();
            fragment.setArguments(new Bundle());
            return fragment;
        }

        @NotNull
        public static InfoFragment get(int deckId) {
            InfoFragment fragment = new InfoFragment();
            Bundle args = new Bundle();
            args.putInt("deckId", deckId);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentNewEditCustomDeckInfoBinding.inflate(inflater, container, false);
            binding.editCustomDeckCollaborators.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

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
                    } else if ((deck == null || !deck.name.equals(str)) && !db.isNameUnique(str)) {
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

            int deckId = requireArguments().getInt("deckId", -1);
            if (deckId == -1) deck = null;
            else deck = db.getDeck(deckId);

            if (deck != null) {
                CommonUtils.setText(binding.editCustomDeckInfoName, deck.name);
                CommonUtils.setText(binding.editCustomDeckInfoWatermark, deck.watermark);
                CommonUtils.setText(binding.editCustomDeckInfoDesc, deck.description);
            }

            OverloadedUtils.waitReady().addOnSuccessListener(signedIn -> {
                if (signedIn) {
                    binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.GONE);
                    binding.editCustomDeckAddCollaborator.setVisibility(View.VISIBLE);
                    binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                    binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);
                    // TODO: Show loading

                    if (deck == null || deck.remoteId == null) {
                        // TODO: Cannot load collaborators yet
                    } else {
                        OverloadedSyncApi.get().getCollaborators(deck.remoteId)
                                .addOnSuccessListener(list -> {
                                    if (list.isEmpty()) {
                                        binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                                        binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.VISIBLE);
                                    } else {
                                        binding.editCustomDeckCollaborators.setVisibility(View.VISIBLE);
                                        binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);

                                        binding.editCustomDeckCollaborators.setAdapter(new CollaboratorsAdapter(list));
                                    }
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed getting collaborators.", ex);
                                    // TODO: Show collaborators error
                                });
                    }
                } else {
                    binding.editCustomDeckAddCollaborator.setVisibility(View.GONE);
                    binding.editCustomDeckCollaborators.setVisibility(View.GONE);
                    binding.editCustomDeckCollaboratorsEmpty.setVisibility(View.GONE);
                    binding.editCustomDeckCollaboratorsOverloaded.setVisibility(View.VISIBLE);
                }
            });

            return binding.getRoot();
        }

        private class CollaboratorsAdapter extends RecyclerView.Adapter<CollaboratorsAdapter.ViewHolder> {
            private final List<String> list;

            CollaboratorsAdapter(@NotNull List<String> list) {
                this.list = list;
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolder(parent);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                String username = list.get(position);
                holder.text.setText(username);

                // TODO: Collaborator popup
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

    private static abstract class CardsFragment extends AbsNewCardsFragment {
        private Integer deckId;
        private CustomDecksDatabase db;

        @Override
        protected final boolean addEnabled() {
            return true;
        }

        @Override
        protected final void onAddClick() {
            // TODO: Show add fragment
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            db = CustomDecksDatabase.get(requireContext());

            deckId = requireArguments().getInt("deckId", -1);
            if (deckId == -1) deckId = null;
        }

        boolean isBlack() {
            return this instanceof BlacksFragment;
        }

        @NotNull
        @Override
        protected final List<? extends BaseCard> getCards(@NotNull Context context) {
            if (deckId == null) return new ArrayList<>();
            else return isBlack() ? db.getBlackCards(deckId) : db.getWhiteCards(deckId);
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
