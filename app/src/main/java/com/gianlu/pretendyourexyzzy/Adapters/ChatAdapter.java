package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.gianlu.commonutils.SuperTextView;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<PollMessage> messages;
    private final LayoutInflater inflater;
    private final Listener handler;

    public ChatAdapter(Context context, Listener handler) {
        this.handler = handler;
        this.messages = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
        if (handler != null) handler.onItemCountChanged(0);
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).timestamp;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PollMessage message = messages.get(position);
        holder.text.setHtml(SuperTextView.makeBold(message.sender) + ": " + message.message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void add(PollMessage message, int gid) {
        if (message.event == PollMessage.Event.CHAT && ((message.gid == -1 && gid == -1) || (gid != -1 && message.gid == gid)))
            messages.add(message);
    }

    public void newMessage(PollMessage message, int gid) {
        synchronized (messages) {
            add(message, gid);
            notifyItemInserted(messages.size() - 1);
        }

        if (handler != null) handler.onItemCountChanged(messages.size());
    }

    public interface Listener {
        void onItemCountChanged(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView text;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_chat, parent, false));
            text = (SuperTextView) itemView;
        }
    }
}
