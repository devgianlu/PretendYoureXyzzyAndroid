package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary.Definition;
import com.gianlu.pretendyourexyzzy.NetIO.UrbanDictionary.Definitions;
import com.gianlu.pretendyourexyzzy.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DefinitionsAdapter extends RecyclerView.Adapter<DefinitionsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Definitions definitions;
    private final Listener listener;

    public DefinitionsAdapter(Context context, Definitions definitions, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.definitions = definitions;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return definitions.get(position).id;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Definition def = definitions.get(position);
        holder.definition.setText(def.definition);
        holder.word.setText(def.word);
        if (def.example.isEmpty()) {
            holder.example.setVisibility(View.GONE);
        } else {
            holder.example.setVisibility(View.VISIBLE);
            holder.example.setText(def.example);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDefinitionSelected(def);
        });
    }

    @Override
    public int getItemCount() {
        return definitions.size();
    }

    public interface Listener {
        void onDefinitionSelected(@NonNull Definition definition);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView definition;
        final TextView word;
        final TextView example;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_urban_def, parent, false));

            definition = itemView.findViewById(R.id.urbanDefItem_definition);
            word = itemView.findViewById(R.id.urbanDefItem_word);
            example = itemView.findViewById(R.id.urbanDefItem_example);
        }
    }
}
