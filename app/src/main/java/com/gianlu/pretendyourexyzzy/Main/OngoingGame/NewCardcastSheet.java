package com.gianlu.pretendyourexyzzy.Main.OngoingGame;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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

import com.gianlu.commonutils.BottomSheet.BaseModalBottomSheet;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.DecksAdapter;
import com.gianlu.pretendyourexyzzy.Main.OngoingGameHelper;
import com.gianlu.pretendyourexyzzy.NetIO.Cardcast;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.List;

public class NewCardcastSheet extends BaseModalBottomSheet implements DecksAdapter.Listener {
    private OngoingGameHelper.Listener listener;
    private RegisteredPyx pyx;
    private RecyclerView list;
    private ViewGroup body;

    @NonNull
    public static NewCardcastSheet get(int gid) {
        NewCardcastSheet sheet = new NewCardcastSheet();
        Bundle args = new Bundle();
        args.putInt("gid", gid);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OngoingGameHelper.Listener)
            listener = (OngoingGameHelper.Listener) context;
    }

    @Override
    protected boolean onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Bundle args) {
        parent.setBackgroundResource(R.color.colorAccent_light);
        inflater.inflate(R.layout.sheet_header_cardcast, parent, true);
        return true;
    }

    @Override
    protected void onRequestedUpdate(@NonNull Object... payloads) {
        List<CardSet> cardSets = (List<CardSet>) payloads[0]; // FIXME
        list.setAdapter(new DecksAdapter(getContext(), cardSets, NewCardcastSheet.this, listener));
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Bundle args) throws MissingArgumentException {
        inflater.inflate(R.layout.sheet_cardcast, parent, true);
        body = parent;

        list = parent.findViewById(R.id.cardcastSheet_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL)); // FIXME

        final int gid = args.getInt("gid", -1);
        if (gid == -1) throw new MissingArgumentException();

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.show(getActivity(), Utils.Messages.FAILED_LOADING, ex);
            dismiss();
            return;
        }

        pyx.request(PyxRequests.listCardcastDecks(gid, Cardcast.get()), new Pyx.OnResult<List<CardSet>>() {
            @Override
            public void onDone(@NonNull List<CardSet> result) {
                list.setAdapter(new DecksAdapter(getContext(), result, NewCardcastSheet.this, listener));
                isLoading(false);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_LOADING, ex);
                dismiss();
            }
        });
    }

    @Override
    protected void onCustomizeToolbar(@NonNull Toolbar toolbar, @NonNull Bundle args) {
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
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Bundle args) {
        if (listener == null || !listener.canModifyCardcastDecks())
            return false;

        action.setImageResource(R.drawable.ic_add_white_48dp);
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
            MessageLayout.show(body, R.string.noCardSets, R.drawable.ic_info_outline_black_48dp);
            list.setVisibility(View.GONE);
        } else {
            MessageLayout.hide(body);
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void removeDeck(@NonNull CardSet deck) {
        Bundle args = getArguments();
        int gid;
        if (args == null || (gid = args.getInt("gid", -1)) == -1 || getContext() == null || deck.cardcastCode == null)
            return;

        pyx.request(PyxRequests.removeCardcastDeck(gid, deck.cardcastCode), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                Toaster.show(getActivity(), Utils.Messages.CARDSET_REMOVED);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_REMOVING_CARDSET, ex);
            }
        });
    }
}
