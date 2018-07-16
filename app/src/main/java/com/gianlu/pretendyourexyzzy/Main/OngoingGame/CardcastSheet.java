package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.gianlu.commonutils.BottomSheet.BaseModalBottomSheet;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.MessageView;
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

import java.util.List;

public class CardcastSheet extends BaseModalBottomSheet<Integer, List<Deck>> implements DecksAdapter.Listener {
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
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OngoingGameHelper.Listener)
            listener = (OngoingGameHelper.Listener) context;
    }

    @Override
    protected boolean onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Integer gid) {
        parent.setBackgroundResource(R.color.colorAccent_light);
        inflater.inflate(R.layout.sheet_header_cardcast, parent, true);
        count = parent.findViewById(R.id.cardcastSheet_count);
        count.setVisibility(View.GONE);
        return true;
    }

    @Override
    protected void onRequestedUpdate(@NonNull List<Deck> decks) {
        list.setAdapter(new DecksAdapter(getContext(), decks, CardcastSheet.this, listener));

        count.setVisibility(View.VISIBLE);
        count.setText(Utils.buildDeckCountString(decks.size(), Deck.countBlackCards(decks), Deck.countWhiteCards(decks)));
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Integer gid) {
        inflater.inflate(R.layout.sheet_cardcast, parent, true);

        message = parent.findViewById(R.id.cardcastSheet_message);
        list = parent.findViewById(R.id.cardcastSheet_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading).ex(ex));
            dismissAllowingStateLoss();
            return;
        }

        pyx.request(PyxRequests.listCardcastDecks(gid, Cardcast.get()), new Pyx.OnResult<List<Deck>>() {
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

    @Override
    protected void onCustomizeToolbar(@NonNull Toolbar toolbar, @NonNull Integer gid) {
        toolbar.setBackgroundResource(R.color.colorAccent_light);
        toolbar.setTitle(R.string.cardcast);
    }

    private void showAddCardcastDeckDialog() {
        if (getContext() == null) return;

        final EditText code = new EditText(getContext());
        code.setHint("XXXXX");
        code.setAllCaps(true);
        code.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5), new InputFilter.AllCaps()});

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.addCardcast)
                .setView(code)
                .setNeutralButton(R.string.addStarred, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) listener.addCardcastStarredDecks();
                    }
                })
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (listener != null) listener.addCardcastDeck(code.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        DialogUtils.showDialog(getActivity(), builder);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Integer gid) {
        if (listener == null || !listener.canModifyCardcastDecks())
            return false;

        action.setImageResource(R.drawable.baseline_add_24);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCardcastDeckDialog();
            }
        });

        return true;
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) {
            message.setInfo(R.string.noCardSets);
            list.setVisibility(View.GONE);
        } else {
            message.hide();
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void removeDeck(@NonNull Deck deck) {
        if (getSetupPayload() == null || deck.cardcastCode == null) return;

        pyx.request(PyxRequests.removeCardcastDeck(getSetupPayload(), deck.cardcastCode), new Pyx.OnSuccess() {
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
}
