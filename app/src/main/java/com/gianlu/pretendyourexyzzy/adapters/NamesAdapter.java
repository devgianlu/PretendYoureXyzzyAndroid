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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class NamesAdapter extends OrderedRecyclerViewAdapter<NamesAdapter.ViewHolder, Name, NamesAdapter.Sorting, String> {
    private final LayoutInflater inflater;
    private final List<String> overloadedUsers;
    private final Listener listener;

    public NamesAdapter(Context context, List<Name> names, @NonNull List<String> overloadedUsers, Listener listener) {
        super(names, Sorting.AZ);
        this.inflater = LayoutInflater.from(context);
        this.overloadedUsers = overloadedUsers;
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
        if (overloadedUsers.contains(name.noSigil()))
            ((SuperTextView) holder.itemView).setTextColor(Color.RED); // TODO
        else
            CommonUtils.setTextColorFromAttr((TextView) holder.itemView, android.R.attr.textColorSecondary);
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
        Iterator<Name> iter = originalObjs.iterator();
        while (iter.hasNext()) {
            if (name.equals(iter.next().noSigil())) {
                iter.remove();
                break;
            }
        }
    }

    public void overloadedUserLeft(@NonNull String nick) {
        if (!overloadedUsers.remove(nick)) return;

        for (Name name : originalObjs) {
            if (name.noSigil().equals(nick)) {
                itemChangedOrAdded(name);
                break;
            }
        }
    }

    public void overloadedUserJoined(@NonNull String nick) {
        if (!overloadedUsers.add(nick)) return;

        for (Name name : originalObjs) {
            if (name.noSigil().equals(nick)) {
                itemChangedOrAdded(name);
                break;
            }
        }
    }

    public void setOverloadedUsers(@NonNull List<String> list) {
        overloadedUsers.clear();
        overloadedUsers.addAll(list);
        notifyDataSetChanged();
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
