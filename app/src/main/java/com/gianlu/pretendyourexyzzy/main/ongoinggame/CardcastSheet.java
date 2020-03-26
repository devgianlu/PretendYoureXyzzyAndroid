package com.gianlu.pretendyourexyzzy.main.ongoinggame;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.activities.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.api.Cardcast;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.main.OngoingGameHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CardcastSheet extends ThemedModalBottomSheet<Integer, List<Deck>> {
    private static final String TAG = CardcastSheet.class.getSimpleName();
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
        parent.setBackgroundColorRes(R.color.colorPrimary);

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
        list.setAdapter(new DecksAdapter(requireContext(), decks, listener));

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
            DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
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
                Log.e(TAG, "Failed getting Cardcast decks.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedLoading));
                dismissAllowingStateLoss();
            }
        });
    }

    private void showAddCardcastDeckDialog() {
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
        action.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cardcastSheetFabBg)));
        action.setOnClickListener(v -> showAddCardcastDeckDialog());
        action.setSupportImageTintList(ColorStateList.valueOf(Color.WHITE));

        return true;
    }

    private void shouldUpdateItemCount(int count) {
        if (count == 0) {
            message.info(R.string.noCardSets);
            list.setVisibility(View.GONE);
        } else {
            message.hide();
            list.setVisibility(View.VISIBLE);
        }
    }

    private void removeDeck(@NonNull Deck deck) {
        if (deck.cardcastCode == null) return;

        pyx.request(PyxRequests.removeCardcastDeck(getSetupPayload(), deck.cardcastCode), getActivity(), new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.cardcastDeckRemoved));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed removing Cardcast deck.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedRemovingCardcastDeck));
            }
        });
    }

    @Override
    protected int getCustomTheme(@NonNull Integer payload) {
        return R.style.AppTheme;
    }

    private class DecksAdapter extends RecyclerView.Adapter<DecksAdapter.ViewHolder> {
        private final List<Deck> sets;
        private final LayoutInflater inflater;
        private final OngoingGameHelper.Listener ongoingGameListener;

        DecksAdapter(@NonNull Context context, List<Deck> sets, OngoingGameHelper.Listener ongoingGameListener) {
            this.sets = sets;
            this.inflater = LayoutInflater.from(context);
            this.ongoingGameListener = ongoingGameListener;

            setHasStableIds(true);
            shouldUpdateItemCount(getItemCount());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public long getItemId(int position) {
            return sets.get(position).id;
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final Deck item = sets.get(position);
            holder.name.setText(Html.fromHtml(item.name));
            holder.whiteCards.setText(String.valueOf(item.whiteCards));
            holder.blackCards.setText(String.valueOf(item.blackCards));

            final CardcastDeck deck = item.cardcastDeck();
            if (deck != null) {
                holder.author.setHtml(R.string.byLowercase, deck.author.username);
                holder.code.setText(deck.code);
                holder.itemView.setOnClickListener(v -> CardcastDeckActivity.startActivity(holder.itemView.getContext(), deck));

                if (ongoingGameListener != null && ongoingGameListener.canModifyCardcastDecks()) {
                    holder.remove.setVisibility(View.VISIBLE);
                    holder.remove.setOnClickListener(v -> {
                        removeDeck(item);
                        remove(holder.getAdapterPosition());
                    });
                } else {
                    holder.remove.setVisibility(View.GONE);
                }
            } else {
                holder.remove.setVisibility(View.GONE);
            }
        }

        private void remove(int pos) {
            if (pos == -1) return;
            sets.remove(pos);
            notifyItemRemoved(pos);
            shouldUpdateItemCount(getItemCount());
        }

        @Override
        public int getItemCount() {
            return sets.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView whiteCards;
            final TextView blackCards;
            final SuperTextView author;
            final TextView code;
            final ImageButton remove;

            ViewHolder(ViewGroup parent) {
                super(inflater.inflate(R.layout.item_cardset, parent, false));

                name = itemView.findViewById(R.id.cardSetItem_name);
                whiteCards = itemView.findViewById(R.id.cardSetItem_whiteCards);
                blackCards = itemView.findViewById(R.id.cardSetItem_blackCards);
                author = itemView.findViewById(R.id.cardSetItem_author);
                code = itemView.findViewById(R.id.cardSetItem_code);
                remove = itemView.findViewById(R.id.cardSetItem_remove);
            }
        }
    }
}
