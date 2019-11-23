package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.activities.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.api.models.CardcastDeck;
import com.gianlu.pretendyourexyzzy.api.models.Deck;
import com.gianlu.pretendyourexyzzy.main.OngoingGameHelper;

import java.util.List;

public class DecksAdapter extends RecyclerView.Adapter<DecksAdapter.ViewHolder> {
    private final List<Deck> sets;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final OngoingGameHelper.Listener ongoingGameListener;

    public DecksAdapter(@NonNull Context context, List<Deck> sets, Listener listener, OngoingGameHelper.Listener ongoingGameListener) {
        this.sets = sets;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.ongoingGameListener = ongoingGameListener;

        setHasStableIds(true);
        listener.shouldUpdateItemCount(getItemCount());
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
                    if (listener != null) listener.removeDeck(item);
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
        listener.shouldUpdateItemCount(getItemCount());
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public interface Listener {
        void shouldUpdateItemCount(int count);

        void removeDeck(@NonNull Deck deck);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
