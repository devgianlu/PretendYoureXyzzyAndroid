package com.gianlu.pretendyourexyzzy.main.chats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;

import java.util.ArrayList;
import java.util.List;

class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<PollMessage> messages;
    private final LayoutInflater inflater;
    private final Listener listener;

    ChatAdapter(@NonNull Context context, @NonNull Listener listener) {
        this.listener = listener;
        this.messages = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
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
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatItemSelected(message.sender);
        });

        if (message.emote) CommonUtils.setTextColor(holder.text, R.color.purple);
        else if (message.wall) CommonUtils.setTextColor(holder.text, R.color.red);
        else CommonUtils.setTextColorFromAttr(holder.text, android.R.attr.textColorSecondary);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @UiThread
    void newMessage(@NonNull PollMessage msg) {
        synchronized (messages) {
            messages.add(msg);
        }

        notifyItemInserted(messages.size() - 1);
        if (listener != null) listener.onItemCountChanged(messages.size());
    }

    public interface Listener {
        void onItemCountChanged(int count);

        void onChatItemSelected(@NonNull String sender);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView text;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_chat, parent, false));
            text = (SuperTextView) itemView;
        }
    }
}
