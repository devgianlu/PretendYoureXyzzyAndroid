package com.gianlu.pretendyourexyzzy.overloaded.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.overloaded.ChatBottomSheet;
import com.gianlu.pretendyourexyzzy.overloaded.api.ChatsCallback;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;

import java.util.List;

public class ChatsFragment extends FragmentWithDialog {

    @NonNull
    public static ChatsFragment get(@NonNull Context context) {
        ChatsFragment fragment = new ChatsFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.chats));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerMessageView rmv = new RecyclerMessageView(requireContext());
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        rmv.startLoading();

        OverloadedApi.get().listChats(getActivity(), new ChatsCallback() {
            @Override
            public void onChats(@NonNull List<OverloadedApi.Chat> chats) {
                if (chats.isEmpty()) {
                    rmv.showInfo(R.string.overloaded_noChats);
                    return;
                }

                rmv.loadListData(new ChatsAdapter(requireContext(), chats));
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                rmv.showError(R.string.failedLoading);
                Logging.log(ex);
            }
        });

        return rmv;
    }

    private class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final List<OverloadedApi.Chat> chats;

        ChatsAdapter(@NonNull Context context, @NonNull List<OverloadedApi.Chat> chats) {
            inflater = LayoutInflater.from(context);
            this.chats = chats;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OverloadedApi.Chat chat = chats.get(position);
            holder.username.setText(chat.getOtherUsername());

            OverloadedApi.ChatMessage lastMsg = chat.lastMsg;
            if (lastMsg == null) {
                holder.lastMsg.setVisibility(View.GONE);
            } else {
                holder.lastMsg.setVisibility(View.VISIBLE);
                holder.lastMsg.setText(lastMsg.text);
            }

            holder.itemView.setOnClickListener(v -> {
                ChatBottomSheet sheet = new ChatBottomSheet();
                sheet.show(getActivity(), chat);
            });
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView username;
            final TextView lastMsg;

            ViewHolder(@NonNull ViewGroup parent) {
                super(inflater.inflate(R.layout.item_overloaded_chat, parent, false));

                username = itemView.findViewById(R.id.overloadedChatItem_user);
                lastMsg = itemView.findViewById(R.id.overloadedChatItem_lastMsg);
            }
        }
    }
}
