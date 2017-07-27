package com.gianlu.pretendyourexyzzy.Adapters;

import android.content.Context;
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
    private final IAdapter handler;

    public ChatAdapter(Context context, IAdapter handler) {
        this.handler = handler;
        this.messages = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
        if (handler != null) handler.onItemCountChanged(0);
        setHasStableIds(true);
    }

    public interface IAdapter {
        void onItemCountChanged(int count);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).timestamp;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PollMessage message = messages.get(position);
        holder.text.setHtml(SuperTextView.makeBold(message.sender) + ": " + message.message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private int add(List<PollMessage> messages) {
        int i = 0;
        for (PollMessage message : messages) {
            if (message.event == PollMessage.Event.CHAT) {
                this.messages.add(message);
                i++;
            }
        }

        return i;
    }

    public void newMessages(List<PollMessage> messages) {
        synchronized (this.messages) {
            notifyItemRangeInserted(this.messages.size() - add(messages), messages.size());
        }

        if (handler != null) handler.onItemCountChanged(this.messages.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView text;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.chat_item, parent, false));
            text = (SuperTextView) itemView;
        }
    }
}
