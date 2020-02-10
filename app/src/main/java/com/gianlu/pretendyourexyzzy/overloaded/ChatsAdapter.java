package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.pretendyourexyzzy.R;

import java.util.List;
import java.util.Map;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final OverloadedApi.ChatModule chat;
    private final List<Map<String, Object>> chats;

    public ChatsAdapter(@NonNull Context context, @NonNull OverloadedApi.ChatModule chat, List<Map<String, Object>> chats) {
        this.inflater = LayoutInflater.from(context);
        this.chat = chat;
        this.chats = chats;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> snap = chats.get(position);
        List<String> users = (List<String>) snap.get("users");
        if (users == null) return;
        users.remove(chat.uid());

        if (users.size() > 1) return;

        holder.username.setText(users.get(0));
        holder.lastMsg.setText("AAAAAAAA");
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView username;
        final TextView lastMsg;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_overloaded_chat, parent, false));

            username = itemView.findViewById(R.id.overloadedChatItem_user);
            lastMsg = itemView.findViewById(R.id.overloadedChatItem_lastMsg);
        }
    }
}
