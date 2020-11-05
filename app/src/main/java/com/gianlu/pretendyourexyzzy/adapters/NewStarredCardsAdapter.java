package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.databinding.ItemStarredCardBinding;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import java.util.List;

public class NewStarredCardsAdapter extends RecyclerView.Adapter<NewStarredCardsAdapter.ViewHolder> {
    private final StarredCardsDatabase db;
    private final List<? extends BaseCard> list;
    private final LayoutInflater inflater;

    public NewStarredCardsAdapter(Context context, @Nullable StarredCardsDatabase db, List<? extends BaseCard> list) {
        this.db = db;
        this.list = list;
        this.inflater = LayoutInflater.from(context);
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BaseCard card = list.get(position);
        holder.binding.starredCardItemText.setHtml(card.textUnescaped());

        if (db != null && card instanceof StarredCardsDatabase.StarredCard) {
            holder.binding.starredCardItemUnstar.setVisibility(View.VISIBLE);
            holder.binding.starredCardItemUnstar.setOnClickListener(v -> {
                db.remove((StarredCardsDatabase.StarredCard) card);

                for (int i = 0; i < list.size(); i++) {
                    if (card.equals(list.get(i))) {
                        list.remove(i);
                        notifyItemRemoved(i);
                        return;
                    }
                }
            });
        } else {
            holder.binding.starredCardItemUnstar.setVisibility(View.GONE);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemStarredCardBinding binding;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_starred_card, parent, false));
            binding = ItemStarredCardBinding.bind(itemView);
        }
    }
}
