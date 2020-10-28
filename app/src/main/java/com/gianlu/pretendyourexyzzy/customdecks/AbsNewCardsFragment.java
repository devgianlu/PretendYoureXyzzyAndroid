package com.gianlu.pretendyourexyzzy.customdecks;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.cards.NewGameCardView;
import com.gianlu.pretendyourexyzzy.databinding.FragmentCustomDeckCardsBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbsNewCardsFragment extends FragmentWithDialog implements AbsNewCustomDeckActivity.SavableFragment {
    private static final int MAX_CARD_TEXT_LENGTH = 256;
    protected CardActionsHandler handler;
    private FragmentCustomDeckCardsBinding binding;
    private BaseCard editCard = null;
    private CardsAdapter adapter;

    @NonNull
    private static ParseResult parseInputText(@NonNull String text, boolean black) {
        text = text.trim();
        if (text.isEmpty() || text.length() > MAX_CARD_TEXT_LENGTH)
            return new ParseResult(ParseResult.Result.ERROR, null, R.string.customCardTextInvalid);

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

        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
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
            hideAdd();
            return true;
        } else {
            return false;
        }
    }

    @Override
    @CallSuper
    public boolean save(@NotNull Bundle bundle, @NotNull Callback callback) {
        if (canEditCards() && isAddVisible()) {
            ParseResult result = parseInputText(binding.customDeckCardsCreateText.getText().toString(), isBlack());
            if (result.result == ParseResult.Result.ERROR || result.output == null)
                return false;

            Task<BaseCard> task;
            if (editCard != null) task = updateCard(editCard, result.output);
            else task = addCard(result.output);

            callback.setLoading(true);
            callback.lockNavigation(true);
            task.addOnCompleteListener(task1 -> {
                callback.setLoading(false);
                callback.lockNavigation(false);

                if (task1.isSuccessful())
                    hideAdd();
            });
            return false;
        }

        return true;
    }

    @Nullable
    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomDeckCardsBinding.inflate(inflater, container, false);
        binding.customDeckCardsList.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false));

        if (canEditCards()) {
            binding.customDeckCardsAddContainer.setVisibility(View.VISIBLE);
            binding.customDeckCardsAdd.setOnClickListener(v -> showAdd(null));

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

                    ParseResult result = parseInputText(s.toString(), isBlack());
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
        hideAdd();

        return binding.getRoot();
    }

    private boolean isAddVisible() {
        return binding.customDeckCardsCreate.getVisibility() == View.VISIBLE;
    }

    private void showAdd(@Nullable BaseCard editCard) {
        binding.customDeckCardsShow.setVisibility(View.GONE);
        binding.customDeckCardsCreate.setVisibility(View.VISIBLE);

        binding.customDeckCardsCreateWatermark.setText(getWatermark());

        if (isBlack()) {
            binding.customDeckCardsCreateCard.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            binding.customDeckCardsCreateText.setTextColor(Color.WHITE);
            binding.customDeckCardsCreateAddImageContainer.setVisibility(View.GONE);

            binding.customDeckCardsCreateHint.setText(R.string.createCustomCardInfo_black);
        } else {
            binding.customDeckCardsCreateCard.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            binding.customDeckCardsCreateText.setTextColor(Color.BLACK);
            binding.customDeckCardsCreateAddImageContainer.setVisibility(View.VISIBLE);

            binding.customDeckCardsCreateHint.setText(R.string.createCustomCardInfo_white);

            binding.customDeckCardsCreateAddImage.setOnClickListener(v -> {
                // TODO: Add image card
            });
        }

        if (editCard != null) {
            this.editCard = editCard;
            binding.customDeckCardsCreateText.setText(editCard.textUnescaped());
        } else {
            this.editCard = null;
        }
    }

    private void hideAdd() {
        binding.customDeckCardsShow.setVisibility(View.VISIBLE);
        binding.customDeckCardsCreate.setVisibility(View.GONE);

        binding.customDeckCardsCreateText.setText(null);

        binding.customDeckCardsCreateText.clearFocus();
        hideKeyboard();

        editCard = null;
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
    private Task<List<? extends BaseCard>> addCards(boolean[] blacks, @NonNull String[][] texts) {
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

        @Nullable
        private String message(@NonNull Context context) {
            return message == 0 ? null : context.getString(message);
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

            if (canEditCards) {
                holder.card.setLeftAction(R.drawable.baseline_edit_24, v -> showAdd(card));
                holder.card.setRightAction(R.drawable.baseline_delete_24, v -> AbsNewCardsFragment.this.removeCard(card));
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
