package com.gianlu.pretendyourexyzzy.customdecks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.gianlu.pretendyourexyzzy.cards.NewGameCardView;
import com.gianlu.pretendyourexyzzy.databinding.DialogAskImageUrlBinding;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewCustomDeckCardsBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedApi.OverloadedServerException;
import xyz.gianlu.pyxoverloaded.model.Card;

public abstract class AbsNewCardsFragment extends FragmentWithDialog implements AbsNewCustomDeckActivity.SavableFragment {
    private static final int MAX_CARD_TEXT_LENGTH = 256;
    private static final int RC_OPEN_CARD_IMAGE = 4;
    private static final String TAG = AbsNewCardsFragment.class.getSimpleName();
    protected CardActionsHandler handler;
    private FragmentNewCustomDeckCardsBinding binding;
    private BaseCard editCard = null;
    private CardsAdapter adapter;
    private OpenCardImageCallback openCardImageCallback;

    @NonNull
    private static ParseResult parseInputText(@NonNull String text, boolean black, boolean image) {
        if (black && image)
            throw new IllegalStateException();

        text = text.trim();
        if (text.isEmpty() || text.length() > MAX_CARD_TEXT_LENGTH)
            return new ParseResult(ParseResult.Result.ERROR, null, R.string.customCardTextInvalid);

        if (image)
            return new ParseResult(ParseResult.Result.OK, new String[]{text}, 0);

        if (black) {
            boolean warn = false;
            String[] output = text.split("____", -1);
            if (output.length == 1) {
                output = new String[]{output[0] + " ", ""};
                return new ParseResult(ParseResult.Result.WARN, output, R.string.missingPlaceholderBlack);
            }

            for (String s : output) {
                if (s.indexOf('_') != -1) {
                    warn = true;
                    break;
                }
            }

            if (warn)
                return new ParseResult(ParseResult.Result.WARN, output, R.string.strangeCustomCardFormat);
            else
                return new ParseResult(ParseResult.Result.OK, output, 0);
        } else {
            String[] output = new String[]{text};
            if (text.indexOf('_') != -1)
                return new ParseResult(ParseResult.Result.WARN, output, R.string.strangeCustomCardFormat);
            else
                return new ParseResult(ParseResult.Result.OK, output, 0);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm == null) return;

        imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
    }

    public final void setHandler(@Nullable CardActionsHandler handler) {
        this.handler = handler;
    }

    @NotNull
    protected abstract String getWatermark();

    protected abstract boolean isBlack();

    protected abstract boolean canEditCards();

    @NotNull
    protected abstract List<? extends BaseCard> getCards(@NotNull Context context);

    public final boolean goBack() {
        if (canEditCards() && isAddVisible()) {
            hideAddEdit();
            return true;
        } else {
            return false;
        }
    }

    @Override
    @CallSuper
    public boolean save(@NotNull Bundle bundle, @NotNull Callback callback) {
        if (canEditCards() && isAddVisible()) {
            ParseResult result = parseInputText(binding.customDeckCardsCreateText.getText().toString(), isBlack(), binding.customDeckCardsCreateImage.getVisibility() == View.VISIBLE);
            if (result.result == ParseResult.Result.ERROR || result.output == null)
                return false;

            Task<BaseCard> task;
            if (editCard != null) task = updateCard(editCard, result.output);
            else task = addCard(result.output);

            callback.lockNavigation(true);
            task.addOnCompleteListener(task1 -> {
                callback.lockNavigation(false);

                if (task1.isSuccessful())
                    hideAddEdit();
            });
            return false;
        }

        return true;
    }

    @Nullable
    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewCustomDeckCardsBinding.inflate(inflater, container, false);
        binding.customDeckCardsList.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false));

        if (canEditCards()) {
            binding.customDeckCardsAddContainer.setVisibility(View.VISIBLE);
            binding.customDeckCardsAdd.setOnClickListener(v -> showAddEdit(null));

            binding.customDeckCardsCreateText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 0) {
                        binding.customDeckCardsCreatePick.setText(null);
                        binding.customDeckCardsCreateMessage.setVisibility(View.GONE);
                        return;
                    }

                    ParseResult result = parseInputText(s.toString(), isBlack(), binding.customDeckCardsCreateImage.getVisibility() == View.VISIBLE);
                    if (result.output != null && isBlack()) {
                        binding.customDeckCardsCreatePick.setHtml(R.string.numPick, result.output.length - 1);
                    } else {
                        binding.customDeckCardsCreatePick.setText(null);
                    }

                    switch (result.result) {
                        default:
                        case OK:
                            binding.customDeckCardsCreateMessage.setVisibility(View.GONE);
                            break;
                        case WARN:
                            binding.customDeckCardsCreateMessage.setVisibility(View.VISIBLE);
                            CommonUtils.setTextColor(binding.customDeckCardsCreateMessage, R.color.orange);
                            binding.customDeckCardsCreateMessage.setText(result.message);
                            break;
                        case ERROR:
                            binding.customDeckCardsCreateMessage.setVisibility(View.VISIBLE);
                            CommonUtils.setTextColor(binding.customDeckCardsCreateMessage, R.color.red);
                            binding.customDeckCardsCreateMessage.setText(result.message);
                            break;
                    }
                }
            });
        } else {
            binding.customDeckCardsAddContainer.setVisibility(View.GONE);
        }

        List<? extends BaseCard> cards = getCards(requireContext());
        binding.customDeckCardsList.setAdapter(adapter = new CardsAdapter(cards, canEditCards()));
        hideAddEdit();

        return binding.getRoot();
    }

    private boolean isAddVisible() {
        return binding != null && binding.customDeckCardsCreate.getVisibility() == View.VISIBLE;
    }

    private void showAddEdit(@Nullable BaseCard editCard) {
        binding.customDeckCardsShow.setVisibility(View.GONE);
        binding.customDeckCardsCreate.setVisibility(View.VISIBLE);

        binding.customDeckCardsCreateWatermark.setText(getWatermark());

        binding.customDeckCardsCreateText.setVisibility(View.VISIBLE);
        binding.customDeckCardsCreateImage.setVisibility(View.GONE);

        if (isBlack()) {
            binding.customDeckCardsCreateCard.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            binding.customDeckCardsCreateText.setTextColor(Color.WHITE);
            binding.customDeckCardsCreateAddImageContainer.setVisibility(View.GONE);

            binding.customDeckCardsCreateHint.setText(R.string.createCustomCardInfo_black);

            binding.customDeckCardsCreateAddImage.setOnClickListener(null);
        } else {
            binding.customDeckCardsCreateCard.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            binding.customDeckCardsCreateText.setTextColor(Color.BLACK);
            binding.customDeckCardsCreateAddImageContainer.setVisibility(View.VISIBLE);

            binding.customDeckCardsCreateHint.setText(R.string.createCustomCardInfo_white);

            binding.customDeckCardsCreateAddImage.setOnClickListener(v -> showAddImageDialog());
        }

        if (editCard != null) {
            this.editCard = editCard;
            binding.customDeckCardsCreateText.setText(editCard.textUnescaped());
        } else {
            this.editCard = null;
        }
    }

    private void hideAddEdit() {
        binding.customDeckCardsShow.setVisibility(View.VISIBLE);
        binding.customDeckCardsCreate.setVisibility(View.GONE);

        binding.customDeckCardsCreateText.setText(null);

        binding.customDeckCardsCreateText.clearFocus();
        hideKeyboard();

        this.editCard = null;
    }

    /**
     * Shows the dialog to add a image card. Image cards cannot be edited, it's pointless.
     */
    private void showAddImageDialog() {
        DialogAskImageUrlBinding dialogBinding = DialogAskImageUrlBinding.inflate(getLayoutInflater());
        dialogBinding.askImageUrlLink.setEndIconOnClickListener(v -> {
            if (!OverloadedUtils.isSignedIn()) {
                showToast(Toaster.build().message(R.string.featureOverloadedOnly));
                return;
            }

            openCardImageCallback = uri -> {
                if (getContext() == null)
                    return;

                ProgressDialog pd = DialogUtils.progressDialog(requireContext(), R.string.loading);
                pd.show();

                try {
                    InputStream in = requireContext().getContentResolver().openInputStream(uri);
                    if (in == null) return;

                    // It will take care of closing the stream
                    OverloadedApi.get().uploadCardImage(in)
                            .addOnSuccessListener(result -> {
                                pd.dismiss();
                                CommonUtils.setText(dialogBinding.askImageUrlLink, OverloadedUtils.getImageUrl(result));
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "Failed uploading image to Overloaded.", ex);

                                pd.dismiss();
                                if (ex instanceof OverloadedServerException && ((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NSFW_DETECTED))
                                    showToast(Toaster.build().message(R.string.nsfwDetectedMessage));
                                else
                                    showToast(Toaster.build().message(R.string.failedUploadingImage));
                            });
                } catch (IOException ex) {
                    Log.e(TAG, "Failed opening image stream.", ex);
                }
            };

            Intent intent = OverloadedUtils.getImageUploadIntent();
            startActivityForResult(Intent.createChooser(intent, "Pick an image to upload..."), RC_OPEN_CARD_IMAGE);
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.addImageCard)
                .setPositiveButton(R.string.done, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(dialogBinding.getRoot());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setEnabled(false);
            button.setOnClickListener(v -> {
                String url = CommonUtils.getText(dialogBinding.askImageUrlLink);
                if (url.isEmpty()) return;

                try {
                    new URL(url);
                } catch (Exception ignored) {
                    return;
                }

                binding.customDeckCardsCreateText.setVisibility(View.GONE);
                binding.customDeckCardsCreateText.setText(String.format("[img]%s[/img]", url));

                binding.customDeckCardsCreateAddImageContainer.setVisibility(View.GONE);
                binding.customDeckCardsCreateImage.setVisibility(View.VISIBLE);
                Glide.with(requireContext()).load(url).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException ex, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Failed loading image.", ex);
                        CommonUtils.setTextColor(binding.customDeckCardsCreateMessage, R.color.red);
                        binding.customDeckCardsCreateMessage.setText(R.string.failedLoadingImage);
                        binding.customDeckCardsCreateMessage.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        binding.customDeckCardsCreateMessage.setVisibility(View.GONE);
                        return false;
                    }
                }).into(binding.customDeckCardsCreateImage);

                di.dismiss();
            });
        });

        CommonUtils.getEditText(dialogBinding.askImageUrlLink).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Button btn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (btn == null) return;

                try {
                    new URL(s.toString());
                    btn.setEnabled(true);
                } catch (Exception ignored) {
                    btn.setEnabled(false);
                }
            }
        });

        showDialog(dialog);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_OPEN_CARD_IMAGE) {
            Uri uri;
            if (openCardImageCallback == null || data == null || (uri = data.getData()) == null)
                return;

            if (resultCode == Activity.RESULT_OK) openCardImageCallback.onImageUri(uri);
            openCardImageCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //region Cards actions
    @NotNull
    private Task<Void> removeCard(@NonNull BaseCard oldCard) {
        if (handler == null) return Tasks.forCanceled();

        return handler.removeCard(oldCard)
                .addOnSuccessListener(aVoid -> {
                    ThisApplication.sendAnalytics(Utils.ACTION_REMOVED_CUSTOM_DECK_CARD);
                    if (adapter != null) adapter.removeCard(oldCard);
                })
                .addOnFailureListener(ex -> showToast(Toaster.build().message(R.string.failedRemovingCard)));
    }

    @NotNull
    private Task<BaseCard> updateCard(@NonNull BaseCard oldCard, @NonNull String[] text) {
        if (handler == null) return Tasks.forCanceled();

        return handler.updateCard(oldCard, text)
                .addOnSuccessListener(card -> {
                    if (adapter != null && card != null)
                        adapter.updateCard(oldCard, card);
                })
                .addOnFailureListener(ex -> showToast(Toaster.build().message(R.string.failedUpdatingCard)));
    }

    @NotNull
    private Task<BaseCard> addCard(@NonNull String[] text) {
        if (handler == null) return Tasks.forCanceled();

        return handler.addCard(isBlack(), text)
                .addOnSuccessListener(card -> {
                    if (card != null) {
                        if (adapter != null) adapter.addCard(card);
                        ThisApplication.sendAnalytics(text[0].startsWith("[img]") && text[0].endsWith("[/img]") ?
                                Utils.ACTION_ADDED_CUSTOM_DECK_IMAGE_CARD : Utils.ACTION_ADDED_CUSTOM_DECK_TEXT_CARD);
                    } else {
                        showToast(Toaster.build().message(R.string.failedAddingCustomCard));
                    }
                })
                .addOnFailureListener(ex -> showToast(Toaster.build().message(R.string.failedAddingCustomCard)));
    }

    @NotNull
    protected Task<List<? extends BaseCard>> addCards(boolean[] blacks, @NonNull String[][] texts) {
        if (handler == null) return Tasks.forCanceled();

        return handler.addCards(blacks, texts)
                .addOnSuccessListener(cards -> {
                    List<BaseCard> filtered = new ArrayList<>(cards);
                    Iterator<BaseCard> iter = filtered.iterator();
                    while (iter.hasNext()) {
                        BaseCard card = iter.next();
                        if (!isBlack() && card.black())
                            iter.remove();
                        else if (isBlack() && !card.black())
                            iter.remove();
                    }

                    if (adapter != null) adapter.addCards(filtered);
                })
                .addOnFailureListener(ex -> showToast(Toaster.build().message(R.string.failedAddingCustomCard)));
    }

    private interface OpenCardImageCallback {
        void onImageUri(@NonNull Uri uri);
    }
    //endregion

    private static class ParseResult {
        final String[] output;
        final ParseResult.Result result;
        final int message;

        ParseResult(@NonNull ParseResult.Result result, @Nullable String[] output, int message) {
            this.output = output;
            this.result = result;
            this.message = message;
        }

        private enum Result {
            OK, WARN, ERROR
        }
    }

    private class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> {
        private final List<BaseCard> cards;
        private final boolean canEditCards;

        CardsAdapter(@NotNull List<? extends BaseCard> cards, boolean canEditCards) {
            this.cards = new ArrayList<>(cards);
            this.canEditCards = canEditCards;

            countUpdated(cards.size());
        }

        void countUpdated(int count) {
            if (count == 0) {
                binding.customDeckCardsEmpty.setVisibility(View.VISIBLE);
                binding.customDeckCardsList.setVisibility(View.GONE);
            } else {
                binding.customDeckCardsEmpty.setVisibility(View.GONE);
                binding.customDeckCardsList.setVisibility(View.VISIBLE);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BaseCard card = cards.get(position);
            holder.card.setCard(card);
            if (card instanceof CustomDecksDatabase.CustomCard || (card instanceof ContentCard && ((ContentCard) card).original instanceof Card)) {
                holder.card.setCustomLeftText((context, card1) -> {
                    String creator = card1 instanceof CustomDecksDatabase.CustomCard ? ((CustomDecksDatabase.CustomCard) card1).creator : ((Card) ((ContentCard) card1).original).creator;


                    return Objects.equals(OverloadedApi.get().username(), creator) ? null : creator;
                });
            } else {
                holder.card.setCustomLeftText(null);
            }

            if (canEditCards) {
                holder.card.setRightAction(R.drawable.baseline_delete_24, v -> AbsNewCardsFragment.this.removeCard(card));

                if (card.getImageUrl() == null)
                    holder.card.setLeftAction(R.drawable.baseline_edit_24, v -> showAddEdit(card));
                else
                    holder.card.unsetLeftAction();
            } else {
                holder.card.unsetRightAction();
                holder.card.unsetLeftAction();
            }
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        public void addCard(@NotNull BaseCard card) {
            cards.add(card);
            notifyItemInserted(cards.size() - 1);
            countUpdated(cards.size());
        }

        public void addCards(@NotNull List<BaseCard> newCards) {
            cards.addAll(newCards);
            notifyItemRangeInserted(cards.size() - newCards.size(), newCards.size());
            countUpdated(cards.size());
        }

        public void removeCard(@NotNull BaseCard card) {
            int index = cards.indexOf(card);
            if (index != -1) {
                cards.remove(index);
                notifyItemRemoved(index);
                countUpdated(cards.size());
            }
        }

        public void updateCard(@NotNull BaseCard oldCard, @NotNull BaseCard card) {
            int index = cards.indexOf(oldCard);
            if (index != -1) {
                cards.set(index, card);
                notifyItemChanged(index);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final NewGameCardView card;

            ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_grid_card, parent, false));
                this.card = (NewGameCardView) ((ViewGroup) itemView).getChildAt(0);
            }
        }
    }
}
