package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.Name;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NamesAdapter extends OrderedRecyclerViewAdapter<NamesAdapter.ViewHolder, Name, NamesAdapter.Sorting, String> {
    private final LayoutInflater inflater;
    private final List<String> overloadedNames;
    private final Listener listener;

    public NamesAdapter(Context context, List<Name> names, @Nullable List<String> overloadedNames, Listener listener) {
        super(names, Sorting.AZ);
        this.inflater = LayoutInflater.from(context);
        this.overloadedNames = overloadedNames == null ? new ArrayList<>() : overloadedNames;
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
    protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name name) {
        ((SuperTextView) holder.itemView).setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));
        holder.itemView.setOnClickListener(v -> listener.onNameSelected(name.noSigil()));
        if (overloadedNames.contains(name.noSigil())) {
            ((SuperTextView) holder.itemView).setTextColor(Color.RED);
        } else {
            CommonUtils.setTextColorFromAttr((TextView) holder.itemView, android.R.attr.textColorSecondary);
        }
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

    public void setOverloadedNames(List<String> list) {
        overloadedNames.clear();
        overloadedNames.addAll(list);
        notifyDataSetChanged(); // FIXME
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
