package com.gianlu.pretendyourexyzzy.Adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.CardcastDeckActivity;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.List;

public class CardSetsAdapter extends RecyclerView.Adapter<CardSetsAdapter.ViewHolder> {
    private final Context context;
    private final int gid;
    private final List<CardSet> sets;
    private final LayoutInflater inflater;
    private final IAdapter listener;
    private final CardcastDeckActivity.IOngoingGame ongoingGameListener;
    private final PYX pyx;

    public CardSetsAdapter(Context context, int gid, List<CardSet> sets, IAdapter listener, CardcastDeckActivity.IOngoingGame ongoingGameListener) {
        this.context = context;
        this.gid = gid;
        this.sets = sets;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.ongoingGameListener = ongoingGameListener;
        this.pyx = PYX.get(context);

        listener.shouldUpdateItemCount(getItemCount());
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
    public void onBindViewHolder(final ViewHolder holder, int position) {
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
                    CardcastDeckActivity.startActivity(context, item.cardcastDeck, ongoingGameListener);
                }
            });

            if (ongoingGameListener != null && ongoingGameListener.canModifyCardcastDecks()) {
                holder.remove.setVisibility(View.VISIBLE);
                holder.remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(context, R.string.loading);
                        CommonUtils.showDialog(context, pd);

                        pyx.removeCardcastCardSet(gid, item.cardcastDeck.code, new PYX.ISuccess() {
                            @Override
                            public void onDone(PYX pyx) {
                                pd.dismiss();
                                sets.remove(holder.getAdapterPosition());
                                Toaster.show(context, Utils.Messages.CARDSET_REMOVED, new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyItemRemoved(holder.getAdapterPosition());
                                        listener.shouldUpdateItemCount(getItemCount());
                                    }
                                });
                            }

                            @Override
                            public void onException(Exception ex) {
                                pd.dismiss();
                                Toaster.show(context, Utils.Messages.FAILED_REMOVING_CARDSET, ex);
                            }
                        });
                    }
                });
            } else {
                holder.remove.setVisibility(View.GONE);
            }
        } else {
            holder.remove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public interface IAdapter {
        void shouldUpdateItemCount(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView whiteCards;
        final TextView blackCards;
        final SuperTextView author;
        final TextView code;
        final ImageButton remove;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_cardset, parent, false));

            name = itemView.findViewById(R.id.cardSetItem_name);
            whiteCards = itemView.findViewById(R.id.cardSetItem_whiteCards);
            blackCards = itemView.findViewById(R.id.cardSetItem_blackCards);
            author = itemView.findViewById(R.id.cardSetItem_author);
            code = itemView.findViewById(R.id.cardSetItem_code);
            remove = itemView.findViewById(R.id.cardSetItem_remove);
        }
    }
}
