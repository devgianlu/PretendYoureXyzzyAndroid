package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
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

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> implements PyxCardsGroupView.ICard {
    private final Context context;
    private final List<CardsGroup<? extends BaseCard>> cards;
    private final IAdapter listener;

    public CardsAdapter(Context context, IAdapter listener) {
        this.context = context;
        this.listener = listener;
        this.cards = new ArrayList<>();
    }

    public CardsAdapter(Context context, List<? extends BaseCard> cards, IAdapter listener) {
        this(context, listener);
        for (BaseCard card : cards) this.cards.add(CardsGroup.singleton(card));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ((PyxCardsGroupView) holder.itemView).setCards(cards.get(position));
        ((PyxCardsGroupView) holder.itemView).setIsFirstOfParent(position == 0);
        ((PyxCardsGroupView) holder.itemView).setIsLastOfParent(position == getItemCount() - 1);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public void notifyDataSetChanged(List<CardsGroup<Card>> whiteCards) {
        this.cards.clear();
        this.cards.addAll(whiteCards);
        notifyDataSetChanged();
    }

    public void notifyItemInserted(List<CardsGroup<Card>> cards) {
        this.cards.addAll(cards);
        notifyItemRangeInserted(this.cards.size() - cards.size(), cards.size());
        notifyItemChanged(this.cards.size() - cards.size() - 1);
    }

    public void notifyItemRemoved(BaseCard removeCard) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            List<? extends BaseCard> subCards = cards.get(i);
            for (BaseCard card : subCards) {
                if (card.getId() == removeCard.getId()) {
                    cards.remove(i);
                    notifyItemRemoved(i);
                    return;
                }
            }
        }
    }

    public void notifyWinningCard(int winnerCardId) {
        for (int i = 0; i < cards.size(); i++) {
            CardsGroup<? extends BaseCard> group = cards.get(i);
            if (group.hasCard(winnerCardId)) {
                RecyclerView list = listener != null ? listener.getCardsRecyclerView() : null;
                if (list != null && list.getLayoutManager() instanceof LinearLayoutManager) { // Scroll only if item is not visible
                    LinearLayoutManager llm = (LinearLayoutManager) list.getLayoutManager();
                    int start = llm.findFirstCompletelyVisibleItemPosition();
                    int end = llm.findLastCompletelyVisibleItemPosition();
                    if (start == -1 || end == -1 || i >= end || i <= start)
                        list.getLayoutManager().smoothScrollToPosition(list, null, i);
                }

                group.setWinner(true);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void addBlankCard() {
        cards.add(CardsGroup.singleton(Card.newBlankCard()));
        notifyItemInserted(cards.size() - 1);
        notifyItemChanged(cards.size() - 2); // Needed to re-compute the margins
    }

    @Override
    public void onCardSelected(BaseCard card) {
        if (listener != null) listener.onCardSelected(card);
    }

    public interface IAdapter {
        @Nullable
        RecyclerView getCardsRecyclerView();

        void onCardSelected(BaseCard card);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder() {
            super(new PyxCardsGroupView(context, CardsAdapter.this));
        }
    }
}
