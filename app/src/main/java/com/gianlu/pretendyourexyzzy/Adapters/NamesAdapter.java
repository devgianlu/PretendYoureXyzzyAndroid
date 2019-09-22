package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Name;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Comparator;
import java.util.List;

public class NamesAdapter extends OrderedRecyclerViewAdapter<NamesAdapter.ViewHolder, Name, NamesAdapter.Sorting, String> {
    private final LayoutInflater inflater;
    private final Listener listener;

    public NamesAdapter(Context context, List<Name> names, Listener listener) {
        super(names, Sorting.AZ);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    @NonNull
    public NamesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    protected boolean matchQuery(@NonNull Name item, @Nullable String query) {
        return query == null || item.withSigil().toLowerCase().contains(query.toLowerCase());
    }

    @Override
    protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull final Name name) {
        ((SuperTextView) holder.itemView).setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));
        holder.itemView.setOnClickListener(v -> listener.onNameSelected(name.noSigil()));
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name payload) {
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

        void shouldUpdateItemCount(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_name, parent, false));
        }
    }
}
