package com.gianlu.pretendyourexyzzy.customdecks.edit;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomCard;
import com.gianlu.pretendyourexyzzy.customdecks.EditCustomDeckActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public abstract class AbsCardsFragment extends FragmentWithDialog implements CardsAdapter.Listener {
    private static final String TAG = AbsCardsFragment.class.getSimpleName();
    private static final int MAX_CARD_TEXT_LENGTH = 128;
    protected CustomDecksDatabase db;
    private Integer id;
    private CardsAdapter adapter;
    private RecyclerMessageView rmv;

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

    @NonNull
    protected abstract List<? extends BaseCard> getCards(int id);

    @NonNull
    private List<? extends BaseCard> loadCards() {
        if (id == null) return new ArrayList<>(0);
        else return getCards(id);
    }

    public void setDeckId(int deckId) {
        id = deckId;
    }

    private boolean isBlack() {
        return this instanceof BlackCardsFragment;
    }

    @NonNull
    private String getWatermark() {
        EditCustomDeckActivity activity = (EditCustomDeckActivity) getActivity();
        return activity == null ? "" : activity.getWatermark();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_custom_cards, container, false);
        FloatingActionButton add = layout.findViewById(R.id.customCardsFragment_add);
        add.setOnClickListener((v) -> showCreateCardDialog(null));

        rmv = layout.findViewById(R.id.customCardsFragment_list);
        rmv.disableSwipeRefresh();
        rmv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rmv.list().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        if (id == null) {
            id = requireArguments().getInt("id", -1);
            if (id == -1) id = null;
        }

        db = CustomDecksDatabase.get(requireContext());
        List<? extends BaseCard> cards = loadCards();
        rmv.loadListData(adapter = new CardsAdapter(true, cards, GameCardView.Action.SELECT, null, true, this), false);
        onItemCountChanged(cards.size());

        return layout;
    }

    private void onItemCountChanged(int count) {
        if (count == 0) rmv.showInfo(R.string.noCustomCards);
        else rmv.showList();
    }

    private void showCreateCardDialog(@Nullable CustomCard oldCard) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_create_card, null, false);
        ((TextView) layout.findViewById(R.id.createCardDialog_info)).setText(isBlack() ? R.string.createCustomCardInfo_black : R.string.createCustomCardInfo_white);
        GameCardView preview = layout.findViewById(R.id.createCardDialog_preview);
        TextInputLayout text = layout.findViewById(R.id.createCardDialog_text);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(oldCard == null ? R.string.createCard : R.string.editCard).setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(oldCard == null ? R.string.add : R.string.save, null);

        if (oldCard != null) {
            builder.setNeutralButton(R.string.remove, (dialog, which) -> {
                if (id != null) {
                    db.removeCard(id, oldCard.id);
                    if (adapter != null) {
                        adapter.removeCard(oldCard);
                        onItemCountChanged(adapter.getItemCount());
                    }
                }
            });
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setEnabled(false);
            button.setOnClickListener(v -> {
                ParseResult result = parseInputText(CommonUtils.getText(text), isBlack());
                if (result.result == ParseResult.Result.ERROR || result.output == null || id == null)
                    return;

                if (oldCard != null) {
                    CustomCard card = db.updateCard(id, oldCard, result.output);
                    if (adapter != null && card != null) {
                        int index = adapter.indexOfGroup(oldCard.id, CustomCard.class, (c, id) -> c.id == id);
                        if (index != -1) adapter.updateCard(index, card);
                    }

                    di.dismiss();
                } else {
                    CustomCard card = db.putCard(id, isBlack(), result.output);
                    if (card != null) {
                        if (adapter != null) {
                            adapter.addCard(card);
                            onItemCountChanged(adapter.getItemCount());
                        }

                        di.dismiss();
                    } else {
                        showToast(Toaster.build().message(R.string.failedAddingCustomCard));
                    }
                }
            });
        });

        CommonUtils.getEditText(text).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ParseResult result = parseInputText(s.toString(), isBlack());

                Button btn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                ColorStateList errorColor;
                switch (result.result) {
                    case OK:
                        errorColor = null;
                        preview.setCard(CustomCard.createTemp(result.output, getWatermark(), isBlack()));
                        if (btn != null) btn.setEnabled(true);
                        break;
                    case WARN:
                        errorColor = ColorStateList.valueOf(ContextCompat.getColor(text.getContext(), R.color.yellow));
                        preview.setCard(CustomCard.createTemp(result.output, getWatermark(), isBlack()));
                        if (btn != null) btn.setEnabled(true);
                        break;
                    case ERROR:
                        errorColor = ColorStateList.valueOf(ContextCompat.getColor(text.getContext(), R.color.red));
                        preview.setCard(null);
                        if (btn != null) btn.setEnabled(false);
                        break;
                    default:
                        throw new IllegalStateException("Unknown result: " + result.result);
                }

                text.setErrorEnabled(errorColor != null);
                text.setErrorTextColor(errorColor);
                text.setErrorIconTintList(errorColor);
                text.setBoxStrokeErrorColor(errorColor);
                text.setError(result.message(text.getContext()));
                text.setHintTextColor(null);
            }
        });

        if (oldCard != null)
            CommonUtils.setText(text, oldCard.text());

        showDialog(dialog);
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        showCreateCardDialog((CustomCard) card);
    }

    public void importCards(@NonNull Context context, @Nullable JSONArray array) {
        if (array == null || id == null) return;

        List<CustomCard> cards = new ArrayList<>(array.length());
        CustomDecksDatabase db = CustomDecksDatabase.get(context);
        for (int i = 0; i < array.length(); i++) {
            try {
                String[] text = CommonUtils.toStringArray(array.getJSONObject(i).getJSONArray("text"));
                CustomCard card = db.putCard(id, isBlack(), text);
                if (card != null) cards.add(card);
                else Log.w(TAG, "Failed saving card to database.");
            } catch (JSONException ex) {
                Log.w(TAG, "Failed importing card at " + i, ex);
            }
        }

        if (adapter != null) adapter.addCardsAsSingleton(cards);
    }

    private static class ParseResult {
        final String[] output;
        final Result result;
        final int message;

        ParseResult(@NonNull Result result, @Nullable String[] output, int message) {
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
}
