package com.gianlu.pretendyourexyzzy.overloaded;

import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.typography.MaterialColors;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedChatApi;
import xyz.gianlu.pyxoverloaded.callback.ChatMessageCallback;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;

public class ChatBottomSheet extends ThemedModalBottomSheet<Chat, ChatBottomSheet.Update> implements OverloadedApi.EventListener {
    public static final int RC_REFRESH_LIST = 5;
    private static final String TAG = ChatBottomSheet.class.getSimpleName();
    private RecyclerMessageView rmv;
    private TextInputLayout send;
    private ChatMessagesAdapter adapter;
    private OverloadedChatApi chatApi;

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView header, @NonNull Chat payload) {
        header.setTitle(payload.recipient);
        header.setBackgroundColorRes(MaterialColors.getShuffledInstance().next());
    }

    private void send() {
        String text = CommonUtils.getText(send);
        if (text.isEmpty() || (text = text.trim()).isEmpty() || chatApi == null)
            return;

        send.setEnabled(false);
        chatApi.sendMessage(getSetupPayload().id, text, getActivity(), new ChatMessageCallback() {
            @Override
            public void onMessage(@NonNull PlainChatMessage msg) {
                CommonUtils.setText(send, "");
                send.setEnabled(true);
            }

            @Override
            public void onFailed(@NotNull Exception ex) {
                Log.e(TAG, "Failed sending message.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSendMessage).extra(getSetupPayload().id));
                send.setEnabled(true);
            }
        });
    }

    @Override
    protected boolean onCreateNoScrollBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Chat payload) {
        inflater.inflate(R.layout.sheet_overloaded_chat, parent, true);
        send = parent.findViewById(R.id.chatSheet_input);
        send.setEndIconOnClickListener(v -> send());
        send.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (rmv != null)
                rmv.setPadding(rmv.getPaddingLeft(), rmv.getPaddingTop(), rmv.getPaddingRight(), v.getHeight());
        });

        EditText sendEditText = CommonUtils.getEditText(send);
        sendEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }

            return false;
        });

        rmv = parent.findViewById(R.id.chatSheet_list);
        rmv.linearLayoutManager(RecyclerView.VERTICAL, true);
        rmv.loadListData(adapter = new ChatMessagesAdapter(), false);
        rmv.startLoading();

        rmv.list().addOnScrollListener(new RecyclerView.OnScrollListener() {
            volatile boolean isLoading = false;
            boolean willLoadMore = true;

            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (!isLoading && willLoadMore && adapter != null && !view.canScrollVertically(-1) && dy < 0) {
                    isLoading = true;
                    List<PlainChatMessage> list = chatApi.getLocalMessages(payload.id, adapter.olderTimestamp());
                    if (list != null && !list.isEmpty()) update(Update.loadedMore(list));
                    else willLoadMore = false;
                    isLoading = false;
                }
            }
        });

        isLoading(false);

        chatApi = OverloadedApi.chat(requireContext());
        List<PlainChatMessage> messages = chatApi.getLocalMessages(payload.id);
        if (!messages.isEmpty()) {
            chatApi.updateLastSeen(chatId(), messages.get(0));
            Fragment fragment = getParentFragment();
            if (fragment != null) fragment.onActivityResult(RC_REFRESH_LIST, 0, null);
        }

        adapter.handleUpdate(Update.messages(messages));
        OverloadedApi.get().addEventListener(this);
        return true;
    }

    @Override
    protected void onExpandedStateChanged(@NonNull ModalBottomSheetHeaderView header, boolean expanded) {
        super.onExpandedStateChanged(header, expanded);
        setDraggable(!expanded);
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Chat payload) {
        setHasBody(false);
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

    private int chatId() {
        return getSetupPayload().id;
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) {
        if (event.type == OverloadedApi.Event.Type.CHAT_MESSAGE && event.obj != null) {
            PlainChatMessage msg = (PlainChatMessage) event.obj;
            if (chatId() == msg.chatId) {
                if (isVisible() && chatApi != null) chatApi.updateLastSeen(chatId(), msg);
                update(Update.message(msg));
            }
        }
    }

    static class Update {
        final List<PlainChatMessage> messages;
        final PlainChatMessage message;
        final boolean loadingMore;

        Update(@Nullable List<PlainChatMessage> messages, @Nullable PlainChatMessage message, boolean loadingMore) {
            this.messages = messages;
            this.message = message;
            this.loadingMore = loadingMore;

            if (messages == null && message == null)
                throw new IllegalArgumentException();
        }

        @NonNull
        static Update loadedMore(@NonNull List<PlainChatMessage> messages) {
            return new Update(messages, null, true);
        }

        @NonNull
        static Update messages(@NonNull List<PlainChatMessage> messages) {
            return new Update(messages, null, false);
        }

        @NonNull
        static Update message(@NonNull PlainChatMessage msg) {
            return new Update(null, msg, false);
        }
    }

    private class ChatMessagesAdapter extends OrderedRecyclerViewAdapter<ChatMessagesAdapter.ViewHolder, PlainChatMessage, Void, NotFilterable> {
        private final LayoutInflater inflater;
        private final int dp64;

        ChatMessagesAdapter() {
            super(new ArrayList<>(128), null);
            this.inflater = LayoutInflater.from(requireContext());
            this.dp64 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        protected boolean matchQuery(@NonNull PlainChatMessage item, @Nullable String query) {
            return true;
        }

        private boolean needsHeader(int pos) {
            if (pos + 1 >= objs.size()) return true;

            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(objs.get(pos).timestamp);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTimeInMillis(objs.get(pos + 1).timestamp);

            return cal1.get(Calendar.ERA) != cal2.get(Calendar.ERA) ||
                    cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
                    cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR);
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull PlainChatMessage payload) {
            PlainChatMessage msg = objs.get(position);

            if (needsHeader(position)) {
                holder.header.setVisibility(View.VISIBLE);
                holder.header.setText(new SimpleDateFormat("EEE, dd/MM/yyyy", Locale.getDefault()).format(msg.timestamp));
            } else {
                holder.header.setVisibility(View.GONE);
            }

            holder.text.setText(msg.text);
            if (payload.isFromMe()) {
                holder.parent.setGravity(Gravity.END);
                holder.text.setPaddingRelative(dp64, 0, 0, 0);
            } else {
                holder.parent.setGravity(Gravity.START);
                holder.text.setPaddingRelative(0, 0, dp64, 0);
            }

            holder.time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(payload.timestamp));
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull PlainChatMessage payload) {
            onSetupViewHolder(holder, position, payload);
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            if (count == 0) rmv.showInfo(R.string.beTheFirstToSendAMessage);
            else rmv.showList();
        }

        @NonNull
        @Override
        public Comparator<PlainChatMessage> getComparatorFor(Void sorting) {
            return (o1, o2) -> (int) (o2.timestamp - o1.timestamp);
        }

        void handleUpdate(@NonNull Update update) {
            if (update.messages != null) {
                if (update.loadingMore) itemsAdded(update.messages);
                else itemsChanged(update.messages);
            } else {
                itemChangedOrAdded(update.message);
            }

            if (!update.loadingMore) {
                RecyclerView list = getList();
                if (list != null) list.scrollToPosition(0);
            }
        }

        long olderTimestamp() {
            return objs.isEmpty() ? 0 : objs.get(objs.size() - 1).timestamp;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final SuperTextView text;
            final TextView time;
            final TextView header;
            final LinearLayout parent;

            ViewHolder(@NonNull ViewGroup parent) {
                super(inflater.inflate(R.layout.item_overloaded_chat_message, parent, false));
                header = itemView.findViewById(R.id.overloadedChatMessageItem_header);
                text = itemView.findViewById(R.id.overloadedChatMessageItem_text);
                time = itemView.findViewById(R.id.overloadedChatMessageItem_time);
                this.parent = (LinearLayout) text.getParent();
            }
        }
    }
}
