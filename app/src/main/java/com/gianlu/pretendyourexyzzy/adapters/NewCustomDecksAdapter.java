package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.crcast.CrCastDeck;
import com.gianlu.pretendyourexyzzy.customdecks.BasicCustomDeck;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.customdecks.NewEditCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.customdecks.NewViewCustomDeckActivity;
import com.gianlu.pretendyourexyzzy.databinding.ItemNewCustomDeckBinding;
import com.gianlu.pretendyourexyzzy.dialogs.NewUserInfoDialog;

import java.util.Comparator;
import java.util.List;

public class NewCustomDecksAdapter extends OrderedRecyclerViewAdapter<NewCustomDecksAdapter.ViewHolder, BasicCustomDeck, Void, Void> {
    private final Context context;
    private final LayoutInflater inflater;
    private final Listener listener;

    public NewCustomDecksAdapter(Context context, List<BasicCustomDeck> list, @Nullable Listener listener) {
        super(list, null);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        shouldUpdateItemCount(objs.size());
    }

    @Override
    protected boolean matchQuery(@NonNull BasicCustomDeck item, @Nullable String query) {
        return true;
    }

    @Override
    protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull BasicCustomDeck deck) {
        holder.binding.customDeckItemName.setText(deck.name);
        holder.binding.customDeckItemWatermark.setText(deck.watermark);

        if (deck.owner != null && deck instanceof CustomDecksDatabase.StarredDeck) {
            holder.binding.customDeckItemOwner.setVisibility(View.VISIBLE);
            CommonUtils.setText(holder.binding.customDeckItemOwner, R.string.deckBy, deck.owner);
        } else {
            holder.binding.customDeckItemOwner.setVisibility(View.GONE);
        }

        if (deck instanceof CustomDecksDatabase.StarredDeck) {
            holder.binding.customDeckItemIcon.setVisibility(View.VISIBLE);
            holder.binding.customDeckItemIcon.setImageResource(R.drawable.baseline_star_24);
        } else if (deck instanceof CrCastDeck) {
            holder.binding.customDeckItemIcon.setVisibility(View.VISIBLE);
            if (((CrCastDeck) deck).favorite)
                holder.binding.customDeckItemIcon.setImageResource(R.drawable.baseline_favorite_contacless_24);
            else
                holder.binding.customDeckItemIcon.setImageResource(R.drawable.baseline_contactless_24);
        } else {
            holder.binding.customDeckItemIcon.setVisibility(View.GONE);
        }

        int whiteCards = deck.whiteCardsCount();
        int blackCards = deck.blackCardsCount();
        if (whiteCards != -1 && blackCards != -1)
            CommonUtils.setText(holder.binding.customDeckItemCards, R.string.cardsCountBlackWhite, blackCards, whiteCards);
        else
            CommonUtils.setText(holder.binding.customDeckItemCards, R.string.cardsCount, deck.cardsCount());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = null;
            if (deck instanceof CustomDecksDatabase.CustomDeck)
                intent = NewEditCustomDeckActivity.activityEditIntent(context, (CustomDecksDatabase.CustomDeck) deck);
            else if (deck instanceof CustomDecksDatabase.StarredDeck && deck.owner != null)
                intent = NewViewCustomDeckActivity.activityPublicIntent(context, (CustomDecksDatabase.StarredDeck) deck);
            else if (deck instanceof NewUserInfoDialog.OverloadedCustomDecks && deck.owner != null)
                intent = NewViewCustomDeckActivity.activityPublicIntent(context, (NewUserInfoDialog.OverloadedCustomDecks) deck);
            else if (deck instanceof CrCastDeck)
                intent = NewViewCustomDeckActivity.activityCrCastIntent(context, (CrCastDeck) deck);

            if (intent == null)
                return;

            context.startActivity(intent);
        });
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull BasicCustomDeck payload) {
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (listener != null) listener.shouldUpdateItemCount(count);
    }

    @NonNull
    @Override
    public Comparator<BasicCustomDeck> getComparatorFor(@NonNull Void sorting) {
        return (o1, o2) -> (int) (o1.lastUsed - o2.lastUsed);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    public interface Listener {
        void shouldUpdateItemCount(int count);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemNewCustomDeckBinding binding;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_new_custom_deck, parent, false));
            binding = ItemNewCustomDeckBinding.bind(itemView);
        }
    }
}
