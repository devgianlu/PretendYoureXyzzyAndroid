package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<String> names;

    public NamesAdapter(Context context, List<String> names, Listener listener) {
        this.names = names;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;

        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
    }

    @Override
    @NonNull
    public NamesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull NamesAdapter.ViewHolder holder, int position) {
        final String name = names.get(position);
        holder.text.setText(name);
        holder.mobile.setVisibility(isMobile(name) ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onNameSelected(name);
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
        final TextView text;
        final ImageView mobile;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_name, parent, false));

            text = (TextView) ((ViewGroup) itemView).getChildAt(0);
            mobile = (ImageView) ((ViewGroup) itemView).getChildAt(1);
        }
    }
}
