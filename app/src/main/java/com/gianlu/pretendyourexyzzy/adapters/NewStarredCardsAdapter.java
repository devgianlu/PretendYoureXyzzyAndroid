package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.databinding.ItemStarredCardBinding;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NewStarredCardsAdapter extends RecyclerView.Adapter<NewStarredCardsAdapter.ViewHolder> {
    private final List<? extends BaseCard> list;
    private final LayoutInflater inflater;
    private final int actionRes;
    private final Listener listener;

    public NewStarredCardsAdapter(Context context, List<? extends BaseCard> list, @DrawableRes int actionRes, Listener listener) {
        this.list = list;
        this.inflater = LayoutInflater.from(context);
        this.actionRes = actionRes;
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BaseCard card = list.get(position);
        holder.binding.starredCardItemText.setHtml(card.textUnescaped());

        if (listener != null && actionRes != 0) {
            holder.binding.starredCardItemAction.setVisibility(View.VISIBLE);
            holder.binding.starredCardItemAction.setImageResource(actionRes);
            holder.binding.starredCardItemAction.setOnClickListener(v -> listener.onCardAction(this, card));
        } else {
            holder.binding.starredCardItemAction.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void removeCard(@NotNull BaseCard card) {
        for (int i = 0; i < list.size(); i++) {
            if (card.equals(list.get(i))) {
                list.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public interface Listener {
        void onCardAction(@NotNull NewStarredCardsAdapter adapter, @NotNull BaseCard card);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemStarredCardBinding binding;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_starred_card, parent, false));
            binding = ItemStarredCardBinding.bind(itemView);
        }
    }
}
