package com.gianlu.pretendyourexyzzy.overloaded;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.databinding.ActivityChatsListBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemOverloadedChatBinding;
import com.gianlu.pretendyourexyzzy.dialogs.NewChatDialog;
import com.gianlu.pretendyourexyzzy.dialogs.NewUserInfoDialog;

import java.util.Comparator;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedChatApi;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;

public class ChatsListActivity extends ActivityWithDialog implements OverloadedApi.EventListener {
    private ActivityChatsListBinding binding;
    private OverloadedChatApi chatApi;
    private ChatsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatsListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.chatsListActivityList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        binding.chatsListActivityBack.setOnClickListener(v -> onBackPressed());
        binding.chatsListActivityMenu.setOnClickListener(v -> {
            // TODO: Chats list popup
        });

        if (!OverloadedUtils.isSignedIn()) {
            finishAfterTransition();
            return;
        }

        chatApi = OverloadedApi.chat(this);
        OverloadedApi.get().addEventListener(this);

        adapter = new ChatsAdapter(chatApi.listChats());
        binding.chatsListActivityList.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OverloadedApi.get().removeEventListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: Refresh chats
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) {
        if (event.type == OverloadedApi.Event.Type.CHAT_MESSAGE && event.obj != null) {
            int chatId = ((PlainChatMessage) event.obj).chatId;
            if (adapter != null) adapter.refresh(chatId);
        }
    }

    private class ChatsAdapter extends OrderedRecyclerViewAdapter<ChatsAdapter.ViewHolder, Chat, Void, Void> {
        ChatsAdapter(@NonNull List<Chat> chats) {
            super(chats, null);
        }

        @NonNull
        @Override
        public ChatsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChatsAdapter.ViewHolder(parent);
        }

        void refresh(int chatId) {
            for (int i = 0; i < objs.size(); i++) {
                Chat chat = objs.get(i);
                if (chat.id == chatId) {
                    notifyItemChanged(i);
                    return;
                }
            }

            if (chatApi != null) {
                Chat chat = chatApi.getChat(chatId);
                if (chat != null) itemChangedOrAdded(chat);
            }
        }

        @Override
        protected boolean matchQuery(@NonNull Chat item, @Nullable String query) {
            return true;
        }

        @Override
        protected void onSetupViewHolder(@NonNull ChatsAdapter.ViewHolder holder, int position, @NonNull Chat payload) {
            Chat chat = objs.get(position);
            holder.binding.overloadedChatItemUser.setText(chat.recipient);

            PlainChatMessage lastMsg = chatApi.getLastMessage(chat.id);
            if (lastMsg == null) {
                holder.binding.overloadedChatItemTime.setVisibility(View.GONE);
                holder.binding.overloadedChatItemLastMsg.setHtml(SuperTextView.makeItalic(getString(R.string.beTheFirstToSendAMessage)));
            } else {
                holder.binding.overloadedChatItemLastMsg.setText(lastMsg.text);
                holder.binding.overloadedChatItemTime.setVisibility(View.VISIBLE);
                holder.binding.overloadedChatItemTime.setText(String.valueOf(lastMsg.timestamp)); // FIXME
            }

            int unread = chatApi.countSinceLastSeen(chat.id);
            if (unread == 0) {
                holder.binding.overloadedChatItemUnread.setVisibility(View.GONE);
            } else {
                holder.binding.overloadedChatItemUnread.setVisibility(View.VISIBLE);
                holder.binding.overloadedChatItemUnread.setText(String.valueOf(unread));
            }

            holder.itemView.setOnClickListener(v -> NewChatDialog.getOverloaded(chat).show(getSupportFragmentManager(), null));

            holder.itemView.setOnLongClickListener(v -> {
                showPopup(holder.itemView, chat);
                return true;
            });
        }

        private void showPopup(@NonNull View anchor, @NonNull Chat chat) {
            PopupMenu popup = new PopupMenu(ChatsListActivity.this, anchor);
            popup.inflate(R.menu.item_chat);

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.chatItemMenu_showProfile:
                        NewUserInfoDialog.get(chat.recipient, false, true).show(getSupportFragmentManager(), null);
                        return true;
                    case R.id.chatItemMenu_delete:
                        if (chatApi != null) {
                            chatApi.deleteChat(chat);
                            removeItem(chat);
                        }
                        return true;
                    default:
                        return false;
                }
            });

            CommonUtils.showPopupOffsetDip(ChatsListActivity.this, popup, 32, 0);
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ChatsAdapter.ViewHolder holder, int position, @NonNull Chat payload) {
            // No update performed
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            // TODO
        }

        @NonNull
        @Override
        public Comparator<Chat> getComparatorFor(@NonNull Void sorting) {
            return (o1, o2) -> {
                PlainChatMessage m1 = chatApi.getLastMessage(o1.id);
                if (m1 == null) return 0;

                PlainChatMessage m2 = chatApi.getLastMessage(o2.id);
                if (m2 == null) return 0;

                return (int) (m2.timestamp - m1.timestamp);
            };
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemOverloadedChatBinding binding;

            ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_overloaded_chat, parent, false));
                binding = ItemOverloadedChatBinding.bind(itemView);
            }
        }
    }
}
