package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.view.ViewGroup;

import com.gianlu.pretendyourexyzzy.CardViews.GameCardView;
import com.gianlu.pretendyourexyzzy.CardViews.PyxCardsGroupView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Card;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardsGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> implements PyxCardsGroupView.CardListener {
    private final GameCardView.Action primary;
    private final GameCardView.Action secondary;
    private final List<CardsGroup> cards;
    private final Listener listener;
    private final boolean forGrid;
    private boolean isSelectable;

    public CardsAdapter(@Nullable GameCardView.Action primary, @Nullable GameCardView.Action secondary, @NonNull Listener listener) {
        this.primary = primary;
        this.secondary = secondary;
        this.listener = listener;
        this.cards = new ArrayList<>();
        this.forGrid = false;
    }

    @UiThread
    public CardsAdapter(boolean forGrid, List<? extends BaseCard> cards, @Nullable GameCardView.Action primary, @Nullable GameCardView.Action secondary, boolean isSelectable, @NonNull Listener listener) {
        this.primary = primary;
        this.secondary = secondary;
        this.isSelectable = isSelectable;
        this.listener = listener;
        this.cards = new ArrayList<>();
        this.forGrid = forGrid;

        addCardsAsSingleton(cards);
        notifyDataSetChanged();
    }

    @NonNull
    public List<CardsGroup> getCards() {
        return cards;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.cards.setCards(cards.get(position), primary, secondary, isSelectable, forGrid, holder);
    }

    @Override
    public int getItemCount() {
        return cards.size();
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
    public void addCardsAsSingleton(@NonNull List<? extends BaseCard> cards) {
        for (BaseCard card : cards) this.cards.add(CardsGroup.singleton(card));
        notifyItemRangeInserted(this.cards.size() - cards.size(), cards.size());
    }

    @UiThread
    public void addCard(@NonNull BaseCard card) {
        this.cards.add(CardsGroup.singleton(card));
        notifyItemInserted(this.cards.size() - 1);
    }

    @UiThread
    public void addCardsAsGroup(@NonNull List<BaseCard> cards) {
        this.cards.add(CardsGroup.from(cards));
        notifyItemInserted(this.cards.size() - 1);
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

    @NonNull
    @UiThread
    public List<BaseCard> findAndRemoveFaceUpCards() {
        List<BaseCard> faceUp = new ArrayList<>();

        for (int i = 0; i < cards.size(); i++) {
            CardsGroup group = cards.get(i);
            if (!group.isUnknwon()) {
                faceUp.addAll(group);
                cards.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }

        return faceUp;
    }

    public interface Listener {
        @Nullable
        RecyclerView getCardsRecyclerView();

        void onCardAction(@NonNull GameCardView.Action action, @NonNull CardsGroup group, @NonNull BaseCard card);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final PyxCardsGroupView cards;

        ViewHolder(Context context) {
            super(new PyxCardsGroupView(context, CardsAdapter.this));
            cards = (PyxCardsGroupView) itemView;
        }
    }
}
