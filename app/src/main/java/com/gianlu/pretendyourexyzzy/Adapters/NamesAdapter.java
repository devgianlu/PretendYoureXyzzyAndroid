package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<DocumentSnapshot> mobiles;
    private final List<String> players;
    private final PYX.Servers currentServer;

    public NamesAdapter(Context context, List<String> players, List<DocumentSnapshot> mobiles) {
        this.players = players;
        this.inflater = LayoutInflater.from(context);
        this.mobiles = mobiles;
        this.currentServer = PYX.get(context).server;

        Collections.sort(players, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
    }

    @Override
    public NamesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(NamesAdapter.ViewHolder holder, int position) {
        String name = players.get(position);
        holder.text.setText(name);
        holder.mobile.setVisibility(isMobile(name) ? View.VISIBLE : View.GONE);
    }

    private boolean isMobile(String name) {
        for (DocumentSnapshot snapshot : mobiles)
            if (Objects.equals(snapshot.getString("nickname"), name) && Objects.equals(snapshot.getString("server"), currentServer.name()))
                return true;

        return false;
    }

    @Override
    public int getItemCount() {
        return players.size();
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
