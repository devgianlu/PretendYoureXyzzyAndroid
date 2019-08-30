package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.BottomSheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.BottomSheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.CasualViews.MessageView;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.DecksAdapter;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameHelper;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Deck;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CardcastSheet extends ThemedModalBottomSheet<Integer, List<Deck>> implements DecksAdapter.Listener {
    private OngoingGameHelper.Listener listener;
    private RegisteredPyx pyx;
    private RecyclerView list;
    private TextView count;
    private MessageView message;

    @NonNull
    public static CardcastSheet get() {
        return new CardcastSheet();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OngoingGameHelper.Listener)
            listener = (OngoingGameHelper.Listener) context;
    }

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull Integer gid) {
        parent.setBackgroundColorRes(R.color.colorPrimary); // FIXME

        inflater.inflate(R.layout.sheet_header_cardcast, parent, true);
        count = parent.findViewById(R.id.cardcastSheet_count);
        count.setVisibility(View.GONE);
    }

    @Override
    protected void onExpandedStateChanged(@NonNull ModalBottomSheetHeaderView header, boolean expanded) {
        ImageView icon = header.findViewById(R.id.cardcastSheet_icon);
        if (icon != null) icon.setVisibility(expanded ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onReceivedUpdate(@NonNull List<Deck> decks) {
        list.setAdapter(new DecksAdapter(requireContext(), decks, CardcastSheet.this, listener));

        count.setVisibility(View.VISIBLE);
        count.setText(Utils.buildDeckCountString(decks.size(), Deck.countBlackCards(decks), Deck.countWhiteCards(decks)));
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Integer gid) {
        inflater.inflate(R.layout.sheet_cardcast, parent, true);

        message = parent.findViewById(R.id.cardcastSheet_message);
        list = parent.findViewById(R.id.cardcastSheet_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
            dismissAllowingStateLoss();
            return;
        }

        pyx.request(PyxRequests.listCardcastDecks(gid, Cardcast.get()), getActivity(), new Pyx.OnResult<List<Deck>>() {
            @Override
            public void onDone(@NonNull List<Deck> result) {
                update(result);
                isLoading(false);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
                dismissAllowingStateLoss();
            }
        });
    }

    private void showAddCardcastDeckDialog() { // FIXME
        if (getContext() == null) return;

        final EditText code = new EditText(getContext());
        code.setHint("XXXXX");
        code.setAllCaps(true);
        code.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5), new InputFilter.AllCaps()});

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.addCardcast)
                .setView(code)
                .setNeutralButton(R.string.addStarred, (dialog, which) -> {
                    if (listener != null) listener.addCardcastStarredDecks();
                })
                .setPositiveButton(R.string.add, (dialogInterface, i) -> {
                    if (listener != null) listener.addCardcastDeck(code.getText().toString());
                })
                .setNegativeButton(android.R.string.cancel, null);

        DialogUtils.showDialog(getActivity(), builder);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Integer gid) {
        if (listener == null || !listener.canModifyCardcastDecks())
            return false;

        action.setImageResource(R.drawable.baseline_add_24);
        action.setOnClickListener(v -> showAddCardcastDeckDialog());
        action.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));

        return true;
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) {
            message.info(R.string.noCardSets);
            list.setVisibility(View.GONE);
        } else {
            message.hide();
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void removeDeck(@NonNull Deck deck) {
        if (deck.cardcastCode == null) return;

        pyx.request(PyxRequests.removeCardcastDeck(getSetupPayload(), deck.cardcastCode), getActivity(), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.cardcastDeckRemoved));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedRemovingCardcastDeck).ex(ex));
            }
        });
    }

    @Override
    protected int getCustomTheme(@NonNull Integer payload) {
        return R.style.AppTheme;
    }
}
