package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.typography.MaterialColors;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.overloaded.api.ChatMessageCallback;
import com.gianlu.pretendyourexyzzy.overloaded.api.ChatMessagesCallback;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChatBottomSheet extends ThemedModalBottomSheet<OverloadedApi.Chat, ChatBottomSheet.Update> {
    private RecyclerMessageView rmv;
    private TextInputLayout send;
    private ChatMessagesAdapter adapter;

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView header, @NonNull OverloadedApi.Chat payload) {
        header.setTitle(payload.getOtherUsername());
        header.setBackgroundColorRes(MaterialColors.getShuffledInstance().next());
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull OverloadedApi.Chat payload) {
        inflater.inflate(R.layout.sheet_overloaded_chat, parent, true);
        send = parent.findViewById(R.id.chatSheet_input);
        send.setEndIconOnClickListener(v -> {
            String text = CommonUtils.getText(send);
            if (text.isEmpty() || (text = text.trim()).isEmpty())
                return;

            send.setEnabled(false);
            OverloadedApi.get().sendMessage(payload.id, text, getActivity(), new ChatMessageCallback() {
                @Override
                public void onMessage(@NonNull OverloadedApi.ChatMessage msg) {
                    update(Update.sent(msg));
                    CommonUtils.setText(send, "");
                    send.setEnabled(true);
                }

                @Override
                public void onFailed(@NotNull Exception ex) {
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSendMessage).ex(ex).extra(payload.id));
                    send.setEnabled(true);
                }
            });
        });

        rmv = parent.findViewById(R.id.chatSheet_list);
        rmv.linearLayoutManager(RecyclerView.VERTICAL, true);
        rmv.loadListData(adapter = new ChatMessagesAdapter(requireContext()), false);
        rmv.startLoading();

        isLoading(false);

        OverloadedApi.get().getMessages(payload.id, getActivity(), new ChatMessagesCallback() {
            @Override
            public void onMessages(@NonNull List<OverloadedApi.ChatMessage> msg) {
                update(Update.allMessages(msg));
            }

            @Override
            public void onFailed(@NotNull Exception ex) {
                Logging.log(ex);
                rmv.showError(R.string.failedLoading);
            }
        });
    }

    @Override
    protected void onReceivedUpdate(@NonNull Update payload) {
        isLoading(false);

        adapter.handleUpdate(payload);
        if (adapter.messages.isEmpty()) rmv.showInfo(R.string.beTheFirstToSendAMessage);
        else rmv.showList();
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull OverloadedApi.Chat payload) {
        return false;
    }

    @NonNull
    public String chatId() {
        return getSetupPayload().id;
    }

    private static class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final List<OverloadedApi.ChatMessage> messages = new ArrayList<>(100);

        ChatMessagesAdapter(@NonNull Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OverloadedApi.ChatMessage msg = messages.get(position);

            ((SuperTextView) holder.itemView).setText(msg.from + " -> " + msg.text);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        void handleUpdate(@NonNull Update update) {
            notifyItemRangeInserted(0, update.addAll(messages));
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(@NonNull ViewGroup parent) {
                super(inflater.inflate(R.layout.item_overloaded_chat_message, parent, false));
            }
        }
    }

    public static class Update {
        final List<OverloadedApi.ChatMessage> messages;
        final OverloadedApi.ChatMessage message;

        Update(@Nullable List<OverloadedApi.ChatMessage> messages, @Nullable OverloadedApi.ChatMessage message) {
            this.messages = messages;
            this.message = message;

            if (messages == null && message == null)
                throw new IllegalArgumentException();
        }

        @NonNull
        static Update allMessages(@NonNull List<OverloadedApi.ChatMessage> messages) {
            return new Update(messages, null);
        }

        @NonNull
        static Update sent(@NonNull OverloadedApi.ChatMessage msg) {
            return new Update(null, msg);
        }

        @NonNull
        public static Update received(@NonNull OverloadedApi.ChatMessage msg) {
            return new Update(null, msg);
        }

        int addAll(@NonNull List<OverloadedApi.ChatMessage> dest) {
            if (messages != null) {
                dest.addAll(0, messages);
                return messages.size();
            } else {
                dest.add(0, message);
                return 1;
            }
        }
    }
}
