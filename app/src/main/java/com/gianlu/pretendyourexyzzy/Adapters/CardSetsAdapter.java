package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class CardSetsAdapter extends RecyclerView.Adapter<CardSetsAdapter.ViewHolder> {
    private final Context context;
    private final List<CardSet> sets;
    private final LayoutInflater inflater;
    private final CardcastDeckActivity.IOngoingGame handler;

    public CardSetsAdapter(Context context, List<CardSet> sets, CardcastDeckActivity.IOngoingGame handler) {
        this.context = context;
        this.sets = sets;
        this.inflater = LayoutInflater.from(context);
        this.handler = handler;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return sets.get(position).id;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CardSet item = sets.get(position);
        holder.name.setText(Html.fromHtml(item.name));
        holder.whiteCards.setText(String.valueOf(item.whiteCards));
        holder.blackCards.setText(String.valueOf(item.blackCards));

        if (item.cardcastDeck != null) {
            holder.author.setHtml(R.string.byLowercase, item.cardcastDeck.author.username);
            holder.code.setText(item.cardcastDeck.code);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CardcastDeckActivity.startActivity(context, item.cardcastDeck, handler);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView whiteCards;
        final TextView blackCards;
        final SuperTextView author;
        final TextView code;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.cardset_item, parent, false));

            name = itemView.findViewById(R.id.cardSetItem_name);
            whiteCards = itemView.findViewById(R.id.cardSetItem_whiteCards);
            blackCards = itemView.findViewById(R.id.cardSetItem_blackCards);
            author = itemView.findViewById(R.id.cardSetItem_author);
            code = itemView.findViewById(R.id.cardSetItem_code);
        }
    }
}
