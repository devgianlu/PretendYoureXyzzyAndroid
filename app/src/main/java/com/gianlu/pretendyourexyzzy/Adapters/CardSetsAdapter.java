package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class CardSetsAdapter extends RecyclerView.Adapter<CardSetsAdapter.ViewHolder> {
    private final List<CardSet> sets;
    private final LayoutInflater inflater;
    private final boolean showDelete;
    private final IAdapter listener;

    public CardSetsAdapter(Context context, List<CardSet> sets, boolean showDelete, IAdapter listener) {
        this.sets = sets;
        this.inflater = LayoutInflater.from(context);
        this.showDelete = showDelete;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CardSet item = sets.get(position);
        holder.name.setText(Html.fromHtml(item.name));

        holder.delete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onDeleteCardSet(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public interface IAdapter {
        void onDeleteCardSet(CardSet item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final ImageButton delete;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.cardset_item, parent, false));

            name = itemView.findViewById(R.id.cardSetItem_name);
            delete = itemView.findViewById(R.id.cardSetItem_delete);
        }
    }
}
