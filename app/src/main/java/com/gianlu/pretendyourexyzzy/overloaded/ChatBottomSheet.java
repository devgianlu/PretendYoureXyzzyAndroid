package com.gianlu.pretendyourexyzzy.overloaded;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.NotFilterable;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.typography.MaterialColors;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.ChatMessageCallback;
import xyz.gianlu.pyxoverloaded.callback.ChatMessagesCallback;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.ChatMessage;
import xyz.gianlu.pyxoverloaded.model.ChatMessages;

public class ChatBottomSheet extends ThemedModalBottomSheet<Chat, ChatBottomSheet.Update> implements OverloadedApi.EventListener {
    public static final int RC_REFRESH_LIST = 5;
    private RecyclerMessageView rmv;
    private TextInputLayout send;
    private ChatMessagesAdapter adapter;

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView header, @NonNull Chat payload) {
        header.setTitle(payload.getOtherUsername());
        header.setBackgroundColorRes(MaterialColors.getShuffledInstance().next());
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Chat payload) {
        inflater.inflate(R.layout.sheet_overloaded_chat, parent, true);
        send = parent.findViewById(R.id.chatSheet_input);
        send.setEndIconOnClickListener(v -> {
            String text = CommonUtils.getText(send);
            if (text.isEmpty() || (text = text.trim()).isEmpty())
                return;

            send.setEnabled(false);
            OverloadedApi.chat().sendMessage(payload.id, text, getActivity(), new ChatMessageCallback() {
                @Override
                public void onMessage(@NonNull ChatMessage msg) {
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
        rmv.loadListData(adapter = new ChatMessagesAdapter(), false);
        rmv.startLoading();

        isLoading(false);

        OverloadedApi.chat().getMessages(payload.id, 0, getActivity(), new ChatMessagesCallback() {
            @Override
            public void onRemoteMessages(@NonNull ChatMessages messages) {
                update(Update.messages(messages));
                if (!messages.isEmpty()) {
                    OverloadedApi.chat().updateLastSeen(chatId(), messages.get(0));

                    Fragment fragment = getParentFragment();
                    if (fragment != null) fragment.onActivityResult(RC_REFRESH_LIST, 0, null);
                }
            }

            @Override
            public void onLocalMessages(@NonNull ChatMessages messages) {
                update(Update.messages(messages));
            }

            @Override
            public void onFailed(@NotNull Exception ex) {
                Logging.log(ex);
                rmv.showError(R.string.failedLoading);
            }
        });

        OverloadedApi.get().addEventListener(this);
    }

    @Override
    protected void onReceivedUpdate(@NonNull Update payload) {
        isLoading(false);
        adapter.handleUpdate(payload);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Chat payload) {
        return false;
    }

    @NonNull
    private String chatId() {
        return getSetupPayload().id;
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == OverloadedApi.Event.Type.CHAT_MESSAGE) {
            String chatId = event.obj.getString("chatId");
            if (chatId().equals(chatId)) {
                ChatMessage msg = new ChatMessage(event.obj);
                if (isVisible()) OverloadedApi.chat().updateLastSeen(chatId(), msg);
                update(Update.message(msg));
            }
        }
    }

    static class Update {
        final List<ChatMessage> messages;
        final ChatMessage message;

        Update(@Nullable List<ChatMessage> messages, @Nullable ChatMessage message) {
            this.messages = messages;
            this.message = message;

            if (messages == null && message == null)
                throw new IllegalArgumentException();
        }

        @NonNull
        static Update messages(@NonNull List<ChatMessage> messages) {
            return new Update(messages, null);
        }

        @NonNull
        static Update message(@NonNull ChatMessage msg) {
            return new Update(null, msg);
        }
    }

    private class ChatMessagesAdapter extends OrderedRecyclerViewAdapter<ChatMessagesAdapter.ViewHolder, ChatMessage, Void, NotFilterable> {
        private final LayoutInflater inflater;

        ChatMessagesAdapter() {
            super(new ArrayList<>(128), null);
            this.inflater = LayoutInflater.from(requireContext());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        protected boolean matchQuery(@NonNull ChatMessage item, @Nullable String query) {
            return true;
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull ChatMessage payload) {
            ChatMessage msg = objs.get(position);
            ((SuperTextView) holder.itemView).setText(msg.from + " -> " + msg.text);
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull ChatMessage payload) {
            onSetupViewHolder(holder, position, payload);
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            if (count == 0) rmv.showInfo(R.string.beTheFirstToSendAMessage);
            else rmv.showList();
        }

        @NonNull
        @Override
        public Comparator<ChatMessage> getComparatorFor(Void sorting) {
            return (o1, o2) -> (int) (o2.timestamp - o1.timestamp);
        }

        void handleUpdate(@NonNull Update update) {
            if (update.messages != null) itemsChanged(update.messages);
            else itemChangedOrAdded(update.message);

            RecyclerView list = getList();
            if (list != null) list.scrollToPosition(0);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(@NonNull ViewGroup parent) {
                super(inflater.inflate(R.layout.item_overloaded_chat_message, parent, false));
            }
        }
    }
}
