package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gianlu.commonutils.Adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Name;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Comparator;
import java.util.List;

public class NamesAdapter extends OrderedRecyclerViewAdapter<NamesAdapter.ViewHolder, Name, NamesAdapter.Sorting, String> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<Name> names;

    public NamesAdapter(Context context, List<Name> names, Listener listener) {
        super(names, Sorting.AZ);
        this.names = names;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    @NonNull
    public NamesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    private boolean isMobile(String name) {
        return false;
    }

    @Nullable
    @Override
    protected RecyclerView getRecyclerView() {
        return listener == null ? null : listener.getRecyclerView();
    }

    @Override
    protected boolean matchQuery(@NonNull Name item, @Nullable String query) {
        return query == null || item.withSigil().toLowerCase().contains(query.toLowerCase());
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull final Name name) {
        holder.name.setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));
        holder.mobile.setVisibility(isMobile(name.noSigil()) ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onNameSelected(name.noSigil());
            }
        });
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (listener != null) listener.shouldUpdateItemCount(count);
    }

    @NonNull
    @Override
    public Comparator<Name> getComparatorFor(Sorting sorting) {
        switch (sorting) {
            default:
            case AZ:
                return new Name.AzComparator();
            case ZA:
                return new Name.ZaComparator();
        }
    }

    public void removeItem(String name) {
        for (int i = 0; i < objs.size(); i++) {
            Name n = objs.get(i);
            if (name.equals(n.noSigil())) {
                removeItem(n);
                break;
            }
        }
    }

    public enum Sorting {
        AZ,
        ZA
    }

    public interface Listener {
        void onNameSelected(@NonNull String name);

        @Nullable
        RecyclerView getRecyclerView();

        void shouldUpdateItemCount(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView name;
        final ImageView mobile;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_name, parent, false));

            name = itemView.findViewById(R.id.nameItem_name);
            mobile = itemView.findViewById(R.id.nameItem_mobile);
        }
    }
}
