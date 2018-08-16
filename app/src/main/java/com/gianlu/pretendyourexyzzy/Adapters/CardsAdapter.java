package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.CardViews.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;

import java.util.ArrayList;
import java.util.List;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> implements PyxCardsGroupView.CardListener {
    private final Context context;
    private final GameCardView.Action primary;
    private final GameCardView.Action secondary;
    private final List<CardsGroup> cards;
    private final Listener listener;
    private final boolean forGrid;
    private boolean isSelectable;

    public CardsAdapter(@NonNull Context context, @Nullable GameCardView.Action primary, @Nullable GameCardView.Action secondary, @NonNull Listener listener) {
        this.context = context;
        this.primary = primary;
        this.secondary = secondary;
        this.listener = listener;
        this.cards = new ArrayList<>();
        this.forGrid = false;
    }

    @UiThread
    public CardsAdapter(@NonNull Context context, boolean forGrid, List<? extends BaseCard> cards, @Nullable GameCardView.Action primary, @Nullable GameCardView.Action secondary, boolean isSelectable, @NonNull Listener listener) {
        this.context = context;
        this.primary = primary;
        this.secondary = secondary;
        this.isSelectable = isSelectable;
        this.listener = listener;
        this.cards = new ArrayList<>();
        this.forGrid = forGrid;
        groupAndNotifyDataSetChanged(cards);
    }

    @NonNull
    public List<CardsGroup> getCards() {
        return cards;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.cards.setCards(cards.get(position), primary, secondary, isSelectable, forGrid, holder);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    @UiThread
    private void groupAndNotifyDataSetChanged(List<? extends BaseCard> cards) {
        this.cards.clear();
        for (BaseCard card : cards) this.cards.add(CardsGroup.singleton(card));
        notifyDataSetChanged();
    }

    public void setSelectable(boolean selectable) {
        isSelectable = selectable;
    }

    @UiThread
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

    @UiThread
    public void addBlankCards(@NonNull BaseCard bc) {
        cards.add(CardsGroup.unknown(bc.numPick()));
        notifyItemInserted(cards.size() - 1);
    }

    @Override
    public void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card) {
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

    @UiThread
    public void addCards(List<Card> cards) {
        for (Card card : cards) this.cards.add(CardsGroup.singleton(card));
        notifyItemRangeInserted(this.cards.size() - cards.size(), cards.size());
    }

    @UiThread
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

    @UiThread
    public void clear() {
        this.cards.clear();
        notifyDataSetChanged();
    }

    @UiThread
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

        void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final PyxCardsGroupView cards;

        ViewHolder() {
            super(new PyxCardsGroupView(context, CardsAdapter.this));
            cards = (PyxCardsGroupView) itemView;
        }
    }
}
