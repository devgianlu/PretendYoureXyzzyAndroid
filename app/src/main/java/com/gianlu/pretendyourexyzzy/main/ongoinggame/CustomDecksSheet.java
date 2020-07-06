package com.gianlu.pretendyourexyzzy.main.ongoinggame;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.DecksAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.FloatingCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.EditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.customdecks.ViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.main.OngoingGameFragment;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.Iterator;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class CustomDecksSheet extends ThemedModalBottomSheet<Integer, List<Deck>> implements DecksAdapter.Listener, Pyx.OnEventListener {
    private static final String TAG = CustomDecksSheet.class.getSimpleName();
    private RegisteredPyx pyx;
    private RecyclerView list;
    private MessageView message;
    private TextView count;
    private DecksAdapter adapter;

    @NonNull
    public static CustomDecksSheet get() {
        return new CustomDecksSheet();
    }

    private static void removeDeck(@NonNull List<FloatingCustomDeck> list, @NonNull Deck deck) {
        Iterator<FloatingCustomDeck> iter = list.iterator();
        while (iter.hasNext()) {
            FloatingCustomDeck d = iter.next();
            if (d.name.equals(deck.name) && d.watermark.equals(deck.watermark)) {
                iter.remove();
                break;
            }
        }
    }

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull Integer gid) {
        parent.setBackgroundColorRes(R.color.colorPrimary);

        inflater.inflate(R.layout.sheet_header_custom_decks, parent, true);
        count = parent.findViewById(R.id.customDecks_count);
        count.setVisibility(View.GONE);
    }

    @Override
    protected void onExpandedStateChanged(@NonNull ModalBottomSheetHeaderView header, boolean expanded) {
        ImageView icon = header.findViewById(R.id.customDecksSheet_icon);
        if (icon != null) icon.setVisibility(expanded ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onReceivedUpdate(@NonNull List<Deck> decks) {
        list.setAdapter(adapter = new DecksAdapter(requireContext(), decks, this));
        isLoading(false);

        count.setVisibility(View.VISIBLE);
        updateCountString();
    }

    private void updateCountString() {
        if (adapter == null) return;

        List<Deck> decks = adapter.getDecks();
        count.setText(Utils.buildDeckCountString(decks.size(), Deck.countBlackCards(decks), Deck.countWhiteCards(decks)));
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Integer gid) {
        inflater.inflate(R.layout.sheet_custom_decks, parent, true);
        message = parent.findViewById(R.id.customDecksSheet_message);
        list = parent.findViewById(R.id.customDecksSheet_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
            dismissAllowingStateLoss();
            return;
        }

        pyx.polling().addListener(this);
        pyx.request(PyxRequests.listCustomDecks(gid), getActivity(), new Pyx.OnResult<List<Deck>>() {
            @Override
            public void onDone(@NonNull List<Deck> result) {
                update(result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed getting custom decks.", ex);

                if (ex instanceof PyxException && ((PyxException) ex).errorCode.equals("bo"))
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDecksNotSupported));
                else
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));

                dismissAllowingStateLoss();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pyx != null) pyx.polling().removeListener(this);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Integer gid) {
        if (!canModifyCustomDecks()) return false;

        action.setImageResource(R.drawable.baseline_add_24);
        action.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.customDecksSheetFabBg)));
        action.setOnClickListener(v -> {
            List<FloatingCustomDeck> customDecks;
            if ((customDecks = getAddableCustomDecks(CustomDecksDatabase.get(requireContext()))).isEmpty())
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.noCustomDecksToAdd));
            else
                showAddCustomDeckDialog(customDecks);
        });
        action.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));
        return true;
    }

    @NonNull
    private List<FloatingCustomDeck> getAddableCustomDecks(@NonNull CustomDecksDatabase db) {
        List<FloatingCustomDeck> customDecks = db.getAllDecks();
        if (adapter != null && adapter.getItemCount() != 0) {
            for (Deck deck : adapter.getDecks())
                removeDeck(customDecks, deck);
        }

        return customDecks;
    }

    private void showAddCustomDeckDialog(@NonNull List<FloatingCustomDeck> customDecks) {
        String[] names = new String[customDecks.size()];
        for (int i = 0; i < names.length; i++) {
            FloatingCustomDeck deck = customDecks.get(i);
            names[i] = deck.name + " (" + deck.watermark + ")";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.addCustomDeck)
                .setNeutralButton(android.R.string.cancel, null)
                .setItems(names, (dialog, which) -> {
                    if (pyx == null || !canModifyCustomDecks()) return;

                    FloatingCustomDeck deck = customDecks.get(which);
                    if (deck instanceof CustomDeck) {
                        String json;
                        try {
                            json = ((CustomDeck) deck).craftPyxJson(CustomDecksDatabase.get(requireContext())).toString();
                        } catch (JSONException ex) {
                            Log.e(TAG, "Failed crating JSON for deck: " + deck.name, ex);
                            return;
                        }

                        ThisApplication.sendAnalytics(Utils.ACTION_ADDED_CUSTOM_DECK);
                        pyx.request(PyxRequests.addCustomDeckJson(getSetupPayload(), json), getActivity(), new Pyx.OnSuccess() {
                            @Override
                            public void onDone() {
                                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDeckAdded));
                            }

                            @Override
                            public void onException(@NonNull Exception ex) {
                                Log.e(TAG, "Failed adding custom deck.", ex);
                                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedAddingCustomDeck));
                            }
                        });
                    } else if (deck instanceof CustomDecksDatabase.StarredDeck) {
                        String url = OverloadedUtils.getServeCustomDeckUrl(((CustomDecksDatabase.StarredDeck) deck).shareCode);

                        ThisApplication.sendAnalytics(Utils.ACTION_ADDED_CUSTOM_DECK);
                        pyx.request(PyxRequests.addCustomDeckUrl(getSetupPayload(), url), getActivity(), new Pyx.OnSuccess() {
                            @Override
                            public void onDone() {
                                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDeckAdded));
                            }

                            @Override
                            public void onException(@NonNull Exception ex) {
                                Log.e(TAG, "Failed adding custom deck: " + url, ex);
                                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedAddingCustomDeck));
                            }
                        });
                    }
                });

        DialogUtils.showDialog(getActivity(), builder);
    }

    @Override
    protected int getCustomTheme(@NonNull Integer payload) {
        return R.style.AppTheme;
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) {
            list.setVisibility(View.GONE);
            message.setVisibility(View.VISIBLE);
            message.info(R.string.noCustomDecks_game);
        } else {
            list.setVisibility(View.VISIBLE);
            message.setVisibility(View.GONE);
        }
    }

    @Override
    public void removeDeck(@NonNull Deck deck) {
        pyx.request(PyxRequests.removeCustomDeck(getSetupPayload(), deck.id), getActivity(), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.removedCustomDeck));
                if (adapter != null) adapter.remove(deck);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedRemovingCustomDeck));
            }
        });
    }

    @Override
    public void onDeckSelected(@NonNull Deck deck) {
        List<CustomDeck> decks = CustomDecksDatabase.get(requireContext()).getDecks();
        for (CustomDeck customDeck : decks) {
            if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark)) {
                EditCustomDeckActivity.startActivityEdit(requireContext(), customDeck);
                return;
            }
        }

        if (OverloadedApi.get().isFullyRegistered())
            ViewCustomDeckActivity.startActivitySearch(requireContext(), deck);
        else
            DialogUtils.showToast(requireContext(), Toaster.build().message(R.string.cannotSearchDeckWithoutOverloaded));
    }

    @Override
    public boolean canModifyCustomDecks() {
        OngoingGameFragment parent = (OngoingGameFragment) getParentFragment();
        return parent != null && parent.canModifyCustomDecks();
    }

    @Override
    public void onPollMessage(@NonNull PollMessage message) throws JSONException {
        if (message.event == PollMessage.Event.ADD_CARDSET) {
            Deck deck = new Deck(message.obj.getJSONObject("cdi"));
            if (adapter != null) {
                adapter.add(deck);
                updateCountString();
            }
        } else if (message.event == PollMessage.Event.REMOVE_CARDSET) {
            Deck deck = new Deck(message.obj.getJSONObject("cdi"));
            if (adapter != null) {
                adapter.remove(deck);
                updateCountString();
            }
        }
    }
}
