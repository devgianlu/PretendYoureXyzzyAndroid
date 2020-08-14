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
import androidx.annotation.Nullable;
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
import com.gianlu.pretendyourexyzzy.api.PyxRequest;
import com.gianlu.pretendyourexyzzy.api.PyxRequestWithResult;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastApi;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.CustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase.StarredDeck;
import com.gianlu.pretendyourexyzzy.customdecks.EditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.customdecks.ViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.main.OngoingGameFragment;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

    private static void removeDeck(@NonNull List<BasicCustomDeck> list, @NonNull Deck deck) {
        Iterator<BasicCustomDeck> iter = list.iterator();
        while (iter.hasNext()) {
            BasicCustomDeck d = iter.next();
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

        PyxRequestWithResult<List<Deck>> req = null;
        if (pyx.config().customDecksEnabled())
            req = PyxRequests.listCustomDecks(gid);
        else if (pyx.config().crCastEnabled())
            req = PyxRequests.listCrCastDecks(gid);

        if (req == null) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDecksNotSupported));
            dismissAllowingStateLoss();
            return;
        }

        pyx.polling().addListener(this);
        pyx.request(req, getActivity(), new Pyx.OnResult<List<Deck>>() {
            @Override
            public void onDone(@NonNull List<Deck> result) {
                update(result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed getting custom decks.", ex);
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

        List<Class<? extends BasicCustomDeck>> supportedDecks = new ArrayList<>(4);
        if (pyx.config().customDecksEnabled())
            supportedDecks.addAll(Arrays.asList(CustomDeck.class, StarredDeck.class, CrCastDeck.class));
        else if (pyx.config().crCastEnabled())
            supportedDecks.add(CrCastDeck.class);

        action.setImageResource(R.drawable.baseline_add_24);
        action.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.customDecksSheetFabBg)));
        action.setOnClickListener(v -> {
            List<BasicCustomDeck> customDecks;
            if ((customDecks = getAddableCustomDecks(CustomDecksDatabase.get(requireContext()), pyx.config().crCastEnabled(), supportedDecks)).isEmpty())
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.noCustomDecksToAdd));
            else
                showAddCustomDeckDialog(customDecks);
        });
        action.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));
        return true;
    }

    @NonNull
    private List<BasicCustomDeck> getAddableCustomDecks(@NonNull CustomDecksDatabase db, boolean needsUrl, @Nullable List<Class<? extends BasicCustomDeck>> supported) {
        if (supported == null || supported.isEmpty()) return Collections.emptyList();

        List<BasicCustomDeck> customDecks = db.getAllDecks();
        Iterator<BasicCustomDeck> iter = customDecks.iterator();
        while (iter.hasNext()) {
            BasicCustomDeck deck = iter.next();
            if (!supported.contains(deck.getClass())) {
                iter.remove();
            } else {
                if (deck instanceof CrCastDeck && !((CrCastDeck) deck).isAccepted() && needsUrl)
                    iter.remove();
            }
        }

        if (adapter != null && adapter.getItemCount() != 0) {
            for (Deck deck : adapter.getDecks())
                removeDeck(customDecks, deck);
        }

        Collections.sort(customDecks, (o1, o2) -> Long.compare(o2.lastUsed, o1.lastUsed));
        return customDecks;
    }

    private void addCustomDeck(@NonNull BasicCustomDeck deck) {
        CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());

        if (pyx.config().customDecksEnabled()) {
            if (deck instanceof CustomDeck) {
                db.updateDeckLastUsed(((CustomDeck) deck).id);

                String json;
                try {
                    json = ((CustomDeck) deck).craftPyxJson(db).toString();
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed crating JSON for deck: " + deck.name, ex);
                    return;
                }

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
            } else if (deck instanceof StarredDeck) {
                db.updateStarredDeckLastUsed(((StarredDeck) deck).id);

                String url = OverloadedUtils.getServeCustomDeckUrl(((StarredDeck) deck).shareCode);
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
            } else if (deck instanceof CrCastDeck) {
                db.updateCrCastDeckLastUsed(deck.watermark, System.currentTimeMillis());

                if (((CrCastDeck) deck).isAccepted()) {
                    String url = CrCastApi.getDeckUrl((CrCastDeck) deck);
                    pyx.request(PyxRequests.addCustomDeckUrl(getSetupPayload(), url), getActivity(), new Pyx.OnSuccess() {
                        @Override
                        public void onDone() {
                            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDeckAdded));
                        }

                        @Override
                        public void onException(@NonNull Exception ex) {
                            Log.e(TAG, "Failed adding CrCast custom deck: " + url, ex);
                            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedAddingCustomDeck));
                        }
                    });
                } else {
                    ((CrCastDeck) deck).getCards(db, new CrCastApi.DeckCallback() {
                        @Override
                        public void onDeck(@NonNull CrCastDeck deck) {
                            String json;
                            try {
                                json = deck.craftPyxJson().toString();
                            } catch (JSONException ex) {
                                Log.e(TAG, "Failed crating JSON for CrCast deck: " + deck.watermark, ex);
                                return;
                            }

                            pyx.request(PyxRequests.addCustomDeckJson(getSetupPayload(), json), getActivity(), new Pyx.OnSuccess() {
                                @Override
                                public void onDone() {
                                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDeckAdded));
                                }

                                @Override
                                public void onException(@NonNull Exception ex) {
                                    Log.e(TAG, "Failed adding CrCast custom deck: " + deck.watermark, ex);
                                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedAddingCustomDeck));
                                }
                            });
                        }

                        @Override
                        public void onException(@NonNull Exception ex) {
                            Log.e(TAG, "Failed loading CrCast deck cards: " + deck.watermark, ex);
                            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedAddingCustomDeck));
                        }
                    });
                }
            } else {
                throw new IllegalStateException(deck.toString());
            }
        } else if (pyx.config().crCastEnabled()) {
            if (deck instanceof CrCastDeck) {
                db.updateCrCastDeckLastUsed(deck.watermark, System.currentTimeMillis());

                pyx.request(PyxRequests.addCrCastDeck(getSetupPayload(), deck.watermark), getActivity(), new Pyx.OnSuccess() {
                    @Override
                    public void onDone() {
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.customDeckAdded));
                    }

                    @Override
                    public void onException(@NonNull Exception ex) {
                        Log.e(TAG, "Failed adding CrCast custom deck: " + deck.watermark, ex);
                        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedAddingCustomDeck));
                    }
                });
            } else {
                throw new IllegalStateException(deck.toString());
            }
        }
    }

    private void showAddCustomDeckDialog(@NonNull List<BasicCustomDeck> customDecks) {
        String[] names = new String[customDecks.size()];
        for (int i = 0; i < names.length; i++) {
            BasicCustomDeck deck = customDecks.get(i);
            names[i] = deck.name + " (" + deck.watermark + ")";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.addCustomDeck)
                .setNeutralButton(android.R.string.cancel, null)
                .setItems(names, (dialog, which) -> {
                    if (pyx == null || !canModifyCustomDecks() || getContext() == null)
                        return;

                    ThisApplication.sendAnalytics(Utils.ACTION_ADDED_CUSTOM_DECK);
                    addCustomDeck(customDecks.get(which));
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
        PyxRequest req = null;
        if (pyx.config().customDecksEnabled())
            req = PyxRequests.removeCustomDeck(getSetupPayload(), deck.id);
        else if (pyx.config().crCastEnabled())
            req = PyxRequests.removeCrCastDeck(getSetupPayload(), deck.id);

        if (req == null)
            return;

        pyx.request(req, getActivity(), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.removedCustomDeck));
                if (adapter != null) adapter.remove(deck);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed removing custom deck.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedRemovingCustomDeck));
            }
        });
    }

    @Override
    public void onDeckSelected(@NonNull Deck deck) {
        CustomDecksDatabase db = CustomDecksDatabase.get(requireContext());

        List<CustomDeck> decks = db.getDecks();
        for (CustomDeck customDeck : decks) {
            if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark)) {
                EditCustomDeckActivity.startActivityEdit(requireContext(), customDeck);
                return;
            }
        }

        List<CrCastDeck> crCastDecks = db.getCachedCrCastDecks();
        for (CrCastDeck customDeck : crCastDecks) {
            if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark)) {
                ViewCustomDeckActivity.startActivityCrCast(requireContext(), customDeck.name, customDeck.watermark);
                return;
            }
        }

        List<StarredDeck> starredDecks = db.getStarredDecks(false);
        for (StarredDeck customDeck : starredDecks) {
            if (customDeck.name.equals(deck.name) && customDeck.watermark.equals(deck.watermark) && customDeck.owner != null) {
                ViewCustomDeckActivity.startActivity(requireContext(), customDeck.owner, customDeck.name, customDeck.shareCode);
                return;
            }
        }

        if (OverloadedUtils.isSignedIn())
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
