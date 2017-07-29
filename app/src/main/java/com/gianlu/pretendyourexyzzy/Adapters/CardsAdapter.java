package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.CardGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> {
    private final Context context;
    private final List<List<Card>> cards;
    private final IAdapter listener;

    public CardsAdapter(Context context, IAdapter listener) {
        this.context = context;
        this.listener = listener;
        this.cards = new ArrayList<>();
    }

    public List<List<Card>> getCards() {
        return cards;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((CardGroupView) holder.itemView).setCards(cards.get(position));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            Object payload = payloads.get(0);
            if (payload instanceof WinningCardDummy) {
                ((CardGroupView) holder.itemView).setWinning();

                RecyclerView list = listener != null ? listener.getCardsRecyclerView() : null;
                if (list != null) list.scrollToPosition(position); // FIXME
            }
        }
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public void notifyDataSetChanged(List<List<Card>> whiteCards) {
        this.cards.clear();
        this.cards.addAll(whiteCards);

        notifyDataSetChanged();
    }

    public void notifyWinningCard(int winnerCardId) {
        for (int i = 0; i < cards.size(); i++) {
            List<Card> subCards = cards.get(i);
            for (Card card : subCards) {
                if (card.id == winnerCardId) {
                    notifyItemChanged(i, new WinningCardDummy());
                    return;
                }
            }
        }
    }

    public void addBlankCard() {
        this.cards.add(Collections.singletonList(Card.newBlankCard()));
        notifyItemInserted(this.cards.size() - 1);
    }

    public interface IAdapter {
        @Nullable
        RecyclerView getCardsRecyclerView();
    }

    private class WinningCardDummy {
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder() {
            super(new CardGroupView(context));
        }
    }
}
