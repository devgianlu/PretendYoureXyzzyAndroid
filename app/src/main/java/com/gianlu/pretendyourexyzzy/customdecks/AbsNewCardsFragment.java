package com.gianlu.pretendyourexyzzy.customdecks;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.cards.NewGameCardView;
import com.gianlu.pretendyourexyzzy.databinding.FragmentCustomDeckCardsBinding;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbsNewCardsFragment extends FragmentWithDialog {

    protected abstract boolean addEnabled();

    @NotNull
    protected abstract List<? extends BaseCard> getCards(@NotNull Context context);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentCustomDeckCardsBinding binding = FragmentCustomDeckCardsBinding.inflate(inflater, container, false);
        binding.customDeckCardsList.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false));

        if (addEnabled()) {
            binding.customDeckCardsAddContainer.setVisibility(View.VISIBLE);
            binding.customDeckCardsAdd.setOnClickListener(v -> {
                // TODO: Show add card fragment
            });
        } else {
            binding.customDeckCardsAddContainer.setVisibility(View.GONE);
        }

        List<? extends BaseCard> cards = getCards(requireContext());
        binding.customDeckCardsList.setAdapter(new CardsAdapter(cards));

        return binding.getRoot();
    }

    private class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.ViewHolder> {
        private final List<? extends BaseCard> cards;

        public CardsAdapter(List<? extends BaseCard> cards) {
            this.cards = cards;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BaseCard card = cards.get(position);
            holder.card.setCard(card);
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final NewGameCardView card;

            ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_grid_card, parent, false));
                this.card = (NewGameCardView) ((ViewGroup) itemView).getChildAt(0);
            }
        }
    }
}
