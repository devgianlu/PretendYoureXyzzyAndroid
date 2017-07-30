package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.Cards.CardGroupView;
import com.gianlu.pretendyourexyzzy.Cards.StarredCardsManager;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> implements CardGroupView.ICard {
    private final Context context;
    private final List<List<? extends BaseCard>> cards;
    private final IAdapter listener;
    private Card associatedBlackCard;

    public CardsAdapter(Context context, IAdapter listener) {
        this.context = context;
        this.listener = listener;
        this.cards = new ArrayList<>();
    }

    public CardsAdapter(Context context, List<? extends BaseCard> cards, IAdapter listener) {
        this(context, listener);
        for (BaseCard card : cards)
            this.cards.add(Collections.singletonList(card));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    public void setAssociatedBlackCard(Card associatedBlackCard) {
        this.associatedBlackCard = associatedBlackCard;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ((CardGroupView) holder.itemView).setAssociatedBlackCard(associatedBlackCard);
        ((CardGroupView) holder.itemView).setCards(cards.get(position));
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
            List<? extends BaseCard> subCards = cards.get(i);
            for (BaseCard card : subCards) {
                if (card.getId() == winnerCardId) {
                    for (BaseCard winningCard : subCards)
                        winningCard.setWinning(true);

                    notifyItemChanged(i);

                    RecyclerView list = listener != null ? listener.getCardsRecyclerView() : null;
                    if (list != null) list.getLayoutManager().smoothScrollToPosition(list, null, i);
                    return;
                }
            }
        }
    }

    public void addBlankCard() {
        this.cards.add(Collections.singletonList(Card.newBlankCard()));
        notifyItemInserted(this.cards.size() - 1);
    }

    @Override
    public void onCardSelected(BaseCard card) {
        if (listener != null) listener.onCardSelected(card);
    }

    @Override
    public void onDeleteCard(StarredCardsManager.StarredCard deleteCard) {
        for (int i = 0; i < cards.size(); i++) {
            List<? extends BaseCard> subCards = cards.get(i);
            for (BaseCard card : subCards) {
                if (card.getId() == deleteCard.getId()) {
                    cards.remove(i);
                    notifyItemRemoved(i);
                    if (listener != null) listener.onDeleteCard(deleteCard);
                    return;
                }
            }
        }
    }

    public interface IAdapter {
        @Nullable
        RecyclerView getCardsRecyclerView();

        void onCardSelected(BaseCard card);

        void onDeleteCard(StarredCardsManager.StarredCard card);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder() {
            super(new CardGroupView(context, CardsAdapter.this));
        }
    }
}
