package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.CardViews.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;

import java.util.ArrayList;
import java.util.List;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> implements PyxCardsGroupView.CardListener {
    private final Context context;
    private final List<CardsGroup> cards;
    private final Listener listener;
    private final PyxCardsGroupView.Action action;
    private final boolean forGrid;

    public CardsAdapter(@NonNull Context context, @Nullable PyxCardsGroupView.Action action, @NonNull Listener listener) {
        this.context = context;
        this.action = action;
        this.listener = listener;
        this.cards = new ArrayList<>();
        this.forGrid = false;
    }

    public CardsAdapter(@NonNull Context context, boolean forGrid, List<? extends BaseCard> cards, @Nullable PyxCardsGroupView.Action action, @NonNull Listener listener) {
        this.context = context;
        this.action = action;
        this.listener = listener;
        this.cards = new ArrayList<>();
        this.forGrid = forGrid;
        groupAndNotifyDataSetChanged(cards);
    }

    @NonNull
    public List<CardsGroup> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards.clear();
        for (Card card : cards) this.cards.add(CardsGroup.singleton(card));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        ((PyxCardsGroupView) holder.itemView).setCards(cards.get(position), action, forGrid, holder);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    private void groupAndNotifyDataSetChanged(List<? extends BaseCard> cards) {
        this.cards.clear();
        for (BaseCard card : cards) this.cards.add(CardsGroup.singleton(card));
        notifyDataSetChanged();
    }

    public void notifyWinningCard(int winnerCardId) {
        for (int i = 0; i < cards.size(); i++) {
            CardsGroup group = cards.get(i);
            if (group.hasCard(winnerCardId)) {
                RecyclerView list = listener != null ? listener.getCardsRecyclerView() : null;
                if (list != null && list.getLayoutManager() instanceof LinearLayoutManager) { // Scroll only if item is not visible
                    LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
                    int start = llm.findFirstCompletelyVisibleItemPosition();
                    int end = llm.findLastCompletelyVisibleItemPosition();
                    if (start == -1 || end == -1 || i >= end || i <= start)
                        list.getLayoutManager().smoothScrollToPosition(list, null, i);
                }

                group.setWinner();
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void addBlankCards(@NonNull BaseCard bc) {
        cards.add(CardsGroup.unknown(bc.numPick()));
        notifyItemInserted(cards.size() - 1);
    }

    @Override
    public void onCardAction(@NonNull PyxCardsGroupView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
        switch (action) {
            case DELETE:
                int pos = cards.indexOf(group);
                if (pos != -1) {
                    cards.remove(pos);
                    notifyItemRemoved(pos);
                }
                break;
        }

        if (listener != null) listener.onCardAction(action, group, card);
    }

    public void addCards(List<Card> cards) {
        for (Card card : cards) this.cards.add(CardsGroup.singleton(card));
        notifyItemRangeInserted(this.cards.size() - cards.size(), cards.size());
    }

    public void setCardGroups(List<CardsGroup> cards, @Nullable BaseCard blackCard) {
        if (blackCard != null) {
            for (CardsGroup group : cards) {
                if (group.isUnknwon()) {
                    for (int i = 1; i < blackCard.numPick(); i++)
                        group.add(Card.newBlankCard());
                }
            }
        }

        this.cards.clear();
        this.cards.addAll(cards);
        notifyDataSetChanged();
    }

    public void clear() {
        this.cards.clear();
        notifyDataSetChanged();
    }

    public void removeCard(@NonNull BaseCard card) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            CardsGroup group = cards.get(i);
            if (group.contains(card)) {
                cards.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public interface Listener {
        @Nullable
        RecyclerView getCardsRecyclerView();

        void onCardAction(@NonNull PyxCardsGroupView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder() {
            super(new PyxCardsGroupView(context, CardsAdapter.this));
        }
    }
}
