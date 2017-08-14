package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class CardSetsAdapter extends RecyclerView.Adapter<CardSetsAdapter.ViewHolder> {
    private final List<CardSet> sets;
    private final LayoutInflater inflater;

    public CardSetsAdapter(Context context, List<CardSet> sets) {
        this.sets = sets;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CardSet item = sets.get(position);
        holder.name.setText(Html.fromHtml(item.name));
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.cardset_item, parent, false));

            name = itemView.findViewById(R.id.cardSetItem_name);
        }
    }
}
