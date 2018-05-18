package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.Cards.CardsGroup;
import com.gianlu.pretendyourexyzzy.Cards.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import java.util.ArrayList;
import java.util.List;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> implements PyxCardsGroupView.CardListener { // FIXME: Can we avoid the margins stuff?
    private final Context context;
    private final List<CardsGroup> cards;
    private final Listener listener;
    private final boolean manageMargins;
    private final PyxCardsGroupView.Action action;

    public CardsAdapter(Context context, boolean manageMargins, @Nullable PyxCardsGroupView.Action action, Listener listener) {
        this.context = context;
        this.manageMargins = manageMargins;
        this.action = action;
        this.listener = listener;
        this.cards = new ArrayList<>();
        setHasStableIds(true);
    }

    public CardsAdapter(Context context, boolean manageMargins, List<? extends BaseCard> cards, PyxCardsGroupView.Action action, Listener listener) {
        this(context, manageMargins, action, listener);
        groupAndNotifyDataSetChanged(cards);
    }

    @Override
    public long getItemId(int position) {
        return cards.get(position).hashCode();
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
        ((PyxCardsGroupView) holder.itemView).setCards(cards.get(position), action);
        if (manageMargins) {
            ((PyxCardsGroupView) holder.itemView).setIsFirstOfParent(position == 0);
            ((PyxCardsGroupView) holder.itemView).setIsLastOfParent(position == getItemCount() - 1);
        }
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
        notifyItemChanged(cards.size() - 2); // Needed to re-compute the margins
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
        notifyItemChanged(this.cards.size() - cards.size() - 1); // Needed to re-compute the margins
    }

    public void setCardGroups(List<CardsGroup> cards) {
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
