package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Name;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Collections;
import java.util.List;

public class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<Name> names;

    public NamesAdapter(Context context, List<Name> names, Listener listener) {
        this.names = names;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;

        Collections.sort(names);
    }

    @Override
    @NonNull
    public NamesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull NamesAdapter.ViewHolder holder, int position) {
        final Name name = names.get(position);
        holder.name.setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));
        holder.mobile.setVisibility(isMobile(name.noSigil()) ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onNameSelected(name.noSigil());
            }
        });
    }

    private boolean isMobile(String name) {
        return false;
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public interface Listener {
        void onNameSelected(@NonNull String name);
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
