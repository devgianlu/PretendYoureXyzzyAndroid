package com.gianlu.pretendyourexyzzy.customdecks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.CardsAdapter;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.models.CardsGroup;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.api.models.cards.ContentCard;
import com.gianlu.pretendyourexyzzy.cards.GameCardView;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomCard;
import com.gianlu.pretendyourexyzzy.dialogs.CardImageZoomDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.GeneralCallback;

public abstract class AbsCardsFragment extends FragmentWithDialog implements CardsAdapter.Listener {
    private static final String TAG = AbsCardsFragment.class.getSimpleName();
    private static final int RC_OPEN_CARD_IMAGE = 4;
    private static final int MAX_CARD_TEXT_LENGTH = 256;
    protected CustomDecksDatabase db;
    protected CardActionsHandler handler;
    private CardsAdapter adapter;
    private RecyclerMessageView rmv;
    private OpenCardImageCallback openCardImageCallback;
    private Listener listener;

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
    protected abstract List<? extends BaseCard> getCards();

    protected abstract boolean editable();

    protected abstract boolean canCollaborate();

    public void setHandler(@NonNull CardActionsHandler handler) {
        this.handler = handler;
    }

    private boolean isBlack() {
        return this instanceof com.gianlu.pretendyourexyzzy.customdecks.edit.BlackCardsFragment
                || this instanceof com.gianlu.pretendyourexyzzy.customdecks.view.BlackCardsFragment;
    }

    @Override
    @CallSuper
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener)
            listener = (Listener) context;
    }

    @Override
    @CallSuper
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @NonNull
    private String getWatermark() {
        return listener != null ? listener.getWatermark() : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_custom_cards, container, false);

        FloatingActionsMenu fab = layout.findViewById(R.id.customCardsFragment_fab);
        if (editable() || canCollaborate()) {
            fab.setVisibility(View.VISIBLE);

            FloatingActionButton addImage = fab.findViewById(R.id.customCardsFragmentFab_addImage);
            FloatingActionButton addText = fab.findViewById(R.id.customCardsFragmentFab_addText);
            addText.setOnClickListener((v) -> {
                showCreateTextCardDialog(null);
                fab.collapse();
            });

            if (isBlack()) {
                fab.removeButton(addImage);
            } else {
                addImage.setOnClickListener((v) -> {
                    showCreateImageCardDialog(null);
                    fab.collapse();
                });
            }
        } else {
            fab.setVisibility(View.GONE);
        }

        rmv = layout.findViewById(R.id.customCardsFragment_list);
        rmv.disableSwipeRefresh();
        rmv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rmv.list().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        db = CustomDecksDatabase.get(requireContext());
        List<? extends BaseCard> cards = getCards();
        rmv.loadListData(adapter = new CardsAdapter(true, cards, (editable() || canCollaborate()) ? GameCardView.Action.SELECT : null, null, true, this), false);
        onItemCountChanged(cards.size());

        return layout;
    }

    private void onItemCountChanged(int count) {
        if (count == 0)
            rmv.showInfo((editable() || canCollaborate()) ? R.string.noCustomCardsCreateOne : R.string.noCustomCards);
        else
            rmv.showList();

        int titleRes = requireArguments().getInt("titleWithCountRes", 0);
        if (titleRes != 0 && getContext() != null) {
            requireArguments().putString("title", getString(titleRes, count));
            if (listener != null) listener.refreshTabs();
        }
    }

    //region Cards actions
    private void removeCard(@NonNull BaseCard oldCard) {
        if (handler == null) return;

        handler.removeCard(oldCard, new CardActionCallback<Void>() {
            @Override
            public void onComplete(Void result) {
                ThisApplication.sendAnalytics(Utils.ACTION_REMOVED_CUSTOM_DECK_CARD);

                if (adapter != null) {
                    adapter.removeCard(oldCard);
                    onItemCountChanged(adapter.getItemCount());
                }
            }

            @Override
            public void onFailed() {
                showToast(Toaster.build().message(R.string.failedRemovingCard));
            }
        });
    }

    private void updateCard(@NonNull DialogInterface di, @NonNull BaseCard oldCard, @NonNull String[] text) {
        if (handler == null) return;

        handler.updateCard(oldCard, text, new CardActionCallback<BaseCard>() {
            @Override
            public void onComplete(BaseCard card) {
                if (adapter != null && card != null) {
                    int index = -1;
                    if (oldCard instanceof CustomCard)
                        index = adapter.indexOfGroup(((CustomCard) oldCard).id, CustomCard.class, (c, id) -> c.id == id);
                    else if (oldCard instanceof ContentCard)
                        index = adapter.indexOfGroup(oldCard.hashCode(), ContentCard.class, (c, id) -> c.hashCode() == id);

                    if (index != -1) adapter.updateCard(index, card);
                }

                di.dismiss();
            }

            @Override
            public void onFailed() {
                showToast(Toaster.build().message(R.string.failedUpdatingCard));
            }
        });
    }

    private void addCard(@NonNull DialogInterface di, @NonNull String[] text, boolean image) {
        if (handler == null) return;

        handler.addCard(isBlack(), text, new CardActionCallback<BaseCard>() {
            @Override
            public void onComplete(BaseCard card) {
                if (card != null) {
                    if (adapter != null) {
                        adapter.addCard(card);
                        onItemCountChanged(adapter.getItemCount());
                    }

                    di.dismiss();
                    ThisApplication.sendAnalytics(image ? Utils.ACTION_ADDED_CUSTOM_DECK_IMAGE_CARD : Utils.ACTION_ADDED_CUSTOM_DECK_TEXT_CARD);
                } else {
                    showToast(Toaster.build().message(R.string.failedAddingCustomCard));
                }
            }

            @Override
            public void onFailed() {
                showToast(Toaster.build().message(R.string.failedAddingCustomCard));
            }
        });
    }

    private void addCards(boolean[] blacks, @NonNull String[][] texts) {
        if (handler == null) return;

        handler.addCards(blacks, texts, new CardActionCallback<List<? extends BaseCard>>() {
            @Override
            public void onComplete(List<? extends BaseCard> cards) {
                List<BaseCard> filter = new ArrayList<>(cards);
                Iterator<BaseCard> iter = filter.iterator();
                while (iter.hasNext()) {
                    BaseCard card = iter.next();
                    if (!isBlack() && card.black())
                        iter.remove();
                    else if (isBlack() && !card.black())
                        iter.remove();
                }

                if (adapter != null) {
                    adapter.addCardsAsSingleton(filter);
                    onItemCountChanged(adapter.getItemCount());
                }
            }

            @Override
            public void onFailed() {
                showToast(Toaster.build().message(R.string.failedAddingCustomCard));
            }
        });
    }
    //endregion

    //region Create card dialogs
    private void showCreateImageCardDialog(@Nullable BaseCard oldCard) {
        if (isBlack())
            return;

        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_create_image_card, null, false);
        ((TextView) layout.findViewById(R.id.createCardDialog_info)).setText(R.string.createCustomCardInfo_image);
        GameCardView preview = layout.findViewById(R.id.createCardDialog_preview);
        preview.setCardImageZoomEnabled(false);
        TextInputLayout imageUrl = layout.findViewById(R.id.createCardDialog_imageLink);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(oldCard == null ? R.string.createCard : R.string.editCard).setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(oldCard == null ? R.string.add : R.string.save, null);

        if (oldCard != null)
            builder.setNeutralButton(R.string.remove, (dialog, which) -> removeCard(oldCard));

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setEnabled(false);
            button.setOnClickListener(v -> {
                String url = CommonUtils.getText(imageUrl);
                if (url.isEmpty()) return;

                try {
                    new URL(url);
                } catch (Exception ignored) {
                    return;
                }

                String[] text = new String[]{"[img]" + url + "[/img]"};
                if (oldCard != null) updateCard(di, oldCard, text);
                else addCard(di, text, true);
            });
        });

        imageUrl.setEndIconOnClickListener(v -> {
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
                    OverloadedApi.get().uploadCardImage(in, null, new GeneralCallback<String>() {
                        @Override
                        public void onResult(@NonNull String result) {
                            pd.dismiss();
                            CommonUtils.setText(imageUrl, OverloadedUtils.getCardImageUrl(result));
                        }

                        @Override
                        public void onFailed(@NonNull Exception ex) {
                            Log.e(TAG, "Failed uploading image to Overloaded.", ex);

                            pd.dismiss();
                            showToast(Toaster.build().message(R.string.failedUploadingImage));
                        }
                    });
                } catch (IOException ex) {
                    Log.e(TAG, "Failed opening image stream.", ex);
                }
            };

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/bmp", "image/gif"});
            startActivityForResult(Intent.createChooser(intent, "Pick an image to upload..."), RC_OPEN_CARD_IMAGE);
        });

        CommonUtils.getEditText(imageUrl).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Button btn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                String urlStr = s.toString();

                try {
                    new URL(urlStr);
                } catch (Exception ignored) {
                    imageUrl.setError(getString(R.string.invalidImageUrl));
                    if (btn != null) btn.setEnabled(false);
                    return;
                }

                preview.setCard(CustomCard.createImageTemp(urlStr, getWatermark(), isBlack()));
                if (btn != null) btn.setEnabled(true);
                imageUrl.setErrorEnabled(false);
            }
        });

        if (oldCard != null) {
            String url = oldCard.getImageUrl();
            if (url != null) CommonUtils.setText(imageUrl, url);
        }

        showDialog(dialog);
    }

    private void showCreateTextCardDialog(@Nullable BaseCard oldCard) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_create_text_card, null, false);
        ((TextView) layout.findViewById(R.id.createCardDialog_info)).setText(isBlack() ? R.string.createCustomCardInfo_black : R.string.createCustomCardInfo_white);
        GameCardView preview = layout.findViewById(R.id.createCardDialog_preview);
        preview.setCardImageZoomEnabled(false);
        TextInputLayout text = layout.findViewById(R.id.createCardDialog_text);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(oldCard == null ? R.string.createCard : R.string.editCard).setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(oldCard == null ? R.string.add : R.string.save, null);

        if (oldCard != null)
            builder.setNeutralButton(R.string.remove, (dialog, which) -> removeCard(oldCard));

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setEnabled(false);
            button.setOnClickListener(v -> {
                ParseResult result = parseInputText(CommonUtils.getText(text), isBlack());
                if (result.result == ParseResult.Result.ERROR || result.output == null)
                    return;

                if (oldCard != null) updateCard(di, oldCard, result.output);
                else addCard(di, result.output, false);
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
    //endregion

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        if (action == GameCardView.Action.SELECT && (editable() || canCollaborate())) {
            if (card.getImageUrl() != null) showCreateImageCardDialog(card);
            else showCreateTextCardDialog(card);
        } else if (action == GameCardView.Action.SELECT_IMG) {
            showDialog(CardImageZoomDialog.get(card));
        }
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

    public interface Listener {
        @NonNull
        String getWatermark();

        void refreshTabs();
    }

    private interface OpenCardImageCallback {
        void onImageUri(@NonNull Uri uri);
    }

    public interface CardActionsHandler {
        void removeCard(@NonNull BaseCard oldCard, @NonNull CardActionCallback<Void> callback);

        void updateCard(@NonNull BaseCard oldCard, @NonNull String[] text, @NonNull CardActionCallback<BaseCard> callback);

        void addCard(boolean black, @NonNull String[] text, @NonNull CardActionCallback<BaseCard> callback);

        void addCards(boolean[] blacks, @NonNull String[][] texts, @NonNull CardActionCallback<List<? extends BaseCard>> callback);
    }

    public interface CardActionCallback<T> {
        void onComplete(T result);

        void onFailed();
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
