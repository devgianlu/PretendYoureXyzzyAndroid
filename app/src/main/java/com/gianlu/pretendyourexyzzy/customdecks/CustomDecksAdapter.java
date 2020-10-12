package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CustomDecksAdapter extends OrderedRecyclerViewAdapter<CustomDecksAdapter.ViewHolder, BasicCustomDeck, Void, Void> {
    private final Listener listener;
    private final LayoutInflater inflater;

    public CustomDecksAdapter(@NonNull Context context, @NonNull List<BasicCustomDeck> decks, @NonNull Listener listener) {
        super(decks, null);
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    protected boolean matchQuery(@NonNull BasicCustomDeck item, @Nullable String query) {
        return true;
    }

    @Override
    protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull BasicCustomDeck deck) {
        holder.name.setText(deck.name);
        holder.watermark.setText(deck.watermark);
        holder.itemView.setOnClickListener(view -> listener.onCustomDeckSelected(deck));
        CommonUtils.setRecyclerViewTopMargin(holder);

        if (deck.owner != null && deck instanceof CustomDecksDatabase.StarredDeck) {
            holder.owner.setVisibility(View.VISIBLE);
            CommonUtils.setText(holder.owner, R.string.deckBy, deck.owner);
        } else {
            holder.owner.setVisibility(View.GONE);
        }

        Drawable startDrawable = null;
        if (deck instanceof CustomDecksDatabase.StarredDeck) {
            startDrawable = holder.name.getContext().getDrawable(R.drawable.baseline_star_24);
        } else if (deck instanceof CrCastDeck) {
            startDrawable = holder.name.getContext().getDrawable(R.drawable.baseline_contactless_24);
        }

        holder.name.setCompoundDrawablesRelativeWithIntrinsicBounds(startDrawable, null, null, null);

        int whiteCards = deck.whiteCardsCount();
        int blackCards = deck.blackCardsCount();
        if (whiteCards != -1 && blackCards != -1)
            CommonUtils.setText(holder.cards, R.string.cardsCountBlackWhite, blackCards, whiteCards);
        else
            CommonUtils.setText(holder.cards, R.string.cardsCount, deck.cardsCount());
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull BasicCustomDeck payload) {
        onSetupViewHolder(holder, position, payload);
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
    }

    @NonNull
    @Override
    public Comparator<BasicCustomDeck> getComparatorFor(Void sorting) {
        return (o1, o2) -> Long.compare(o2.lastUsed, o1.lastUsed);
    }

    public void removeCrCastDeck(@NonNull String deckCode) {
        for (BasicCustomDeck deck : new ArrayList<>(objs)) {
            if (deck instanceof CrCastDeck && deck.watermark.equals(deckCode)) {
                removeItem(deck);
                break;
            }
        }
    }

    public void removeAllCrCastDecks() {
        for (BasicCustomDeck deck : new ArrayList<>(objs))
            if (deck instanceof CrCastDeck) removeItem(deck);
    }

    public interface Listener {
        void onCustomDeckSelected(@NonNull BasicCustomDeck deck);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView watermark;
        final TextView cards;
        final TextView owner;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_custom_deck, parent, false));

            name = itemView.findViewById(R.id.customDeckItem_name);
            watermark = itemView.findViewById(R.id.customDeckItem_watermark);
            cards = itemView.findViewById(R.id.customDeckItem_cards);
            owner = itemView.findViewById(R.id.customDeckItem_owner);
        }
    }
}
