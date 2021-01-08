package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BlockedUsers;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.databinding.DialogNewChatBinding;
import com.gianlu.pretendyourexyzzy.overloaded.ChatsListActivity;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedChatApi;
import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;

public final class NewChatDialog extends DialogFragment {
    private static final String TAG = NewChatDialog.class.getSimpleName();
    private ChatController controller;
    private ChatAdapter adapter;
    private DialogNewChatBinding binding;

    @NotNull
    private static NewChatDialog get(@NotNull Type type, int gid, Chat chat) {
        NewChatDialog dialog = new NewChatDialog();
        Bundle args = new Bundle();
        args.putSerializable("type", type);
        args.putInt("gid", gid);
        if (chat != null) {
            args.putInt("chatId", chat.id);
            args.putString("recipient", chat.recipient);
        }

        dialog.setArguments(args);
        return dialog;
    }

    @NotNull
    public static NewChatDialog getGlobal() {
        return get(Type.GLOBAL, -1, null);
    }

    @NotNull
    public static NewChatDialog getGame(int gid) {
        return get(Type.GAME, gid, null);
    }

    @NotNull
    public static NewChatDialog getOverloaded(@NotNull Chat chat) {
        return get(Type.OVERLOADED, -1, chat);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        Window window;
        if (dialog != null && (window = dialog.getWindow()) != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (controller != null) controller.readAllMessages();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogNewChatBinding.inflate(inflater, container, false);

        Type type = (Type) requireArguments().getSerializable("type");
        if (type == null) {
            dismissAllowingStateLoss();
            return null;
        }

        binding.chatFragmentBack.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.chatFragmentMenu.setVisibility(View.GONE);

        LinearLayoutManager llm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        llm.setStackFromEnd(true);
        binding.chatFragmentList.setLayoutManager(llm);

        switch (type) {
            case GLOBAL:
                binding.chatFragmentTitle.setText(R.string.globalChat);
                controller = PyxController.globalController();
                break;
            case GAME:
                int gid = requireArguments().getInt("gid", -1);
                if (gid == -1) {
                    dismissAllowingStateLoss();
                    return null;
                }

                binding.chatFragmentTitle.setText(R.string.gameChat);
                controller = PyxController.gameController(gid);
                break;
            case OVERLOADED:
                int chatId = requireArguments().getInt("chatId", -1);
                if (chatId == -1) {
                    dismissAllowingStateLoss();
                    return null;
                }

                controller = new OverloadedController(requireContext(), chatId);
                binding.chatFragmentTitle.setText(requireArguments().getString("recipient"));
                break;
            default:
                dismissAllowingStateLoss();
                return null;
        }

        try {
            adapter = new ChatAdapter(controller.init(), controller.showSender());
            binding.chatFragmentList.setAdapter(adapter);
        } catch (Exception ex) {
            Log.e(TAG, "Failed initializing controller.", ex);
            return null;
        }

        AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_OPEN_CHAT);

        controller.attach(msg -> {
            if (adapter != null) {
                adapter.addNewMessage(msg);
                binding.chatFragmentList.scrollToPosition(adapter.getItemCount() - 1);
            }

            controller.readAllMessages();
        });

        binding.chatFragmentText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }

            return false;
        });

        binding.chatFragmentSend.setOnClickListener(v -> send());

        binding.chatFragmentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            volatile boolean isLoading = false;
            boolean willLoadMore = true;

            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (!isLoading && willLoadMore && adapter != null && controller instanceof OverloadedController && !view.canScrollVertically(-1) && dy < 0) {
                    isLoading = true;

                    List<ChatMessage> list = ((OverloadedController) controller).getLocalMessages(adapter.olderTimestamp());
                    if (list != null && !list.isEmpty())
                        handler.post(() -> adapter.addOlderMessages(list));
                    else
                        willLoadMore = false;

                    isLoading = false;
                }
            }
        });

        return binding.getRoot();
    }

    private void send() {
        String msg = binding.chatFragmentText.getText().toString().trim();
        if (msg.isEmpty() || controller == null) return;

        binding.chatFragmentSend.setEnabled(false);
        binding.chatFragmentText.setEnabled(false);
        controller.send(msg, new ChatController.SendCallback() {
            @Override
            public void onSuccessful() {
                binding.chatFragmentSend.setEnabled(true);
                binding.chatFragmentText.setEnabled(true);

                binding.chatFragmentText.setText(null);
            }

            @Override
            public void unknownCommand() {
                DialogUtils.showToast(getContext(), Toaster.build().message(R.string.unknownChatCommand));
                binding.chatFragmentSend.setEnabled(true);
                binding.chatFragmentText.setEnabled(true);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Log.e(TAG, "Failed sending message.", ex);
                binding.chatFragmentSend.setEnabled(true);
                binding.chatFragmentText.setEnabled(true);

                int stringRes;
                if (ex instanceof PyxException) {
                    switch (((PyxException) ex).errorCode) {
                        case "rm":
                            stringRes = R.string.cannotRepeatMessage;
                            break;
                        case "tmsc":
                            stringRes = R.string.tooManySpecialCharacters;
                            break;
                        case "tf":
                            stringRes = R.string.chattingTooFast;
                            break;
                        case "CL":
                            stringRes = R.string.tryCapsLockOff;
                            break;
                        case "rW":
                            stringRes = R.string.mustUseMoreUniqueWords;
                            break;
                        case "mtl":
                            stringRes = R.string.chatMessageTooLong;
                            break;
                        case "nes":
                            stringRes = R.string.tooLessWords;
                            break;
                        default:
                            stringRes = R.string.failedSendMessage;
                    }
                } else {
                    stringRes = R.string.failedSendMessage;
                }

                DialogUtils.showToast(getContext(), Toaster.build().message(stringRes));
            }
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        Activity activity = getActivity();
        if (activity instanceof ChatsListActivity && controller instanceof OverloadedController)
            ((ChatsListActivity) activity).refreshChat(((OverloadedController) controller).chatId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null) controller.detach();
    }

    private enum Type {
        GAME, GLOBAL, OVERLOADED
    }

    private interface ChatController {
        @NotNull
        List<ChatMessage> init() throws LevelMismatchException;

        void send(@NonNull String msg, @NonNull SendCallback callback);

        void readAllMessages();

        @NotNull
        String username();

        void attach(@NonNull Listener listener);

        void detach();

        boolean showSender();

        interface SendCallback {
            void onSuccessful();

            void unknownCommand();

            void onFailed(@NonNull Exception ex);
        }

        interface Listener {
            void onChatMessage(@NonNull ChatMessage msg);
        }
    }

    static class OverloadedController implements ChatController {
        private final OverloadedChatApi api;
        private final int chatId;
        private OverloadedApi.EventListener eventListener;

        OverloadedController(@NotNull Context context, int chatId) {
            this.api = OverloadedApi.chat(context);
            this.chatId = chatId;
        }

        @Nullable
        public List<ChatMessage> getLocalMessages(long since) {
            List<PlainChatMessage> messages = api.getLocalMessages(chatId, since);
            return messages != null ? ChatMessage.fromOverloaded(messages) : null;
        }

        @NotNull
        @Override
        public List<ChatMessage> init() {
            return ChatMessage.fromOverloaded(api.getLocalMessages(chatId));
        }

        @Override
        public void send(@NonNull String msg, @NonNull SendCallback callback) {
            api.sendMessage(chatId, msg)
                    .addOnSuccessListener(msg1 -> callback.onSuccessful())
                    .addOnFailureListener(callback::onFailed);
        }

        @Override
        public void readAllMessages() {
            PlainChatMessage msg = api.getLastMessage(chatId);
            if (msg != null) api.updateLastSeen(chatId, msg);
        }

        @NotNull
        @Override
        public String username() {
            String username = OverloadedApi.get().username();
            if (username == null) throw new IllegalStateException();
            else return username;
        }

        @Override
        public void attach(@NonNull Listener listener) {
            OverloadedApi.get().addEventListener(eventListener = event -> {
                if (event.type == OverloadedApi.Event.Type.CHAT_MESSAGE && event.obj != null) {
                    listener.onChatMessage(ChatMessage.fromOverloaded((PlainChatMessage) event.obj));
                }
            });
        }

        @Override
        public void detach() {
            OverloadedApi.get().removeEventListener(eventListener);
        }

        @Override
        public boolean showSender() {
            return false;
        }
    }

    static class PyxController implements ChatController {
        private final int gid;
        private RegisteredPyx pyx;
        private Pyx.OnEventListener eventListener;

        private PyxController(int gid) {
            this.gid = gid;
        }

        @NonNull
        static PyxController globalController() {
            return new PyxController(-1);
        }

        @NonNull
        static PyxController gameController(int gid) {
            return new PyxController(gid);
        }

        @NotNull
        @Override
        public String username() {
            return pyx.user().nickname;
        }

        @NonNull
        @Override
        public List<ChatMessage> init() throws LevelMismatchException {
            pyx = RegisteredPyx.get();
            return ChatMessage.fromPyx(gid == -1 ? pyx.chat().getMessagesForGlobal() : pyx.chat().getMessagesForGame(gid));
        }

        @Override
        public void attach(@NonNull Listener listener) {
            if (pyx == null) throw new IllegalStateException();

            pyx.polling().addListener(eventListener = msg -> {
                if (msg.sender == null || msg.message == null || msg.event != PollMessage.Event.CHAT)
                    return;

                if ((msg.gid == -1 && gid == -1) || (gid != -1 && msg.gid == gid)) {
                    if (!msg.wall && BlockedUsers.isBlocked(msg.sender))
                        return;

                    listener.onChatMessage(ChatMessage.fromPyx(msg));
                }
            });
        }

        @Override
        public void send(@NonNull String msg, @NonNull SendCallback callback) {
            if (pyx == null) throw new IllegalStateException();

            boolean emote;
            boolean wall;
            if (msg.startsWith("/")) {
                String[] split = msg.split(" ");
                if (split.length == 1 || split[1].isEmpty())
                    return;

                msg = split[1];

                switch (split[0].substring(1)) {
                    case "me":
                        emote = true;
                        wall = false;
                        break;
                    case "wall":
                        emote = false;
                        wall = true;
                        break;
                    default:
                        callback.unknownCommand();
                        return;
                }
            } else {
                emote = false;
                wall = false;
            }

            send(msg, emote, wall)
                    .addOnSuccessListener(aVoid -> {
                        AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_SEND_CHAT);
                        callback.onSuccessful();
                    })
                    .addOnFailureListener(callback::onFailed);
        }

        @NonNull
        private Task<Void> send(@NonNull String msg, boolean emote, boolean wall) {
            if (gid == -1) {
                AnalyticsApplication.sendAnalytics(Utils.ACTION_SENT_MSG);
                return pyx.request(PyxRequests.sendMessage(msg, emote, wall));
            } else {
                AnalyticsApplication.sendAnalytics(Utils.ACTION_SENT_GAME_MSG);
                return pyx.request(PyxRequests.sendGameMessage(gid, msg, emote));
            }
        }

        @Override
        public void detach() {
            if (pyx != null && eventListener != null) pyx.polling().removeListener(eventListener);
        }

        @Override
        public boolean showSender() {
            return true;
        }

        @Override
        public void readAllMessages() {
            if (pyx == null) return;

            if (gid == -1) pyx.chat().resetGlobalUnread(System.currentTimeMillis());
            else pyx.chat().resetGameUnread(System.currentTimeMillis());
        }
    }

    private static class ChatMessage {
        final String sender;
        final String text;
        final long timestamp;

        private ChatMessage(@NotNull String sender, @NotNull String text, long timestamp) {
            this.sender = sender;
            this.text = text;
            this.timestamp = timestamp;
        }

        @NotNull
        static List<ChatMessage> fromPyx(@NotNull List<PollMessage> events) {
            List<ChatMessage> list = new ArrayList<>(events.size());
            for (PollMessage event : events) list.add(fromPyx(event));
            return list;
        }

        @NotNull
        static ChatMessage fromPyx(@NotNull PollMessage event) {
            return new ChatMessage(event.sender, event.message, event.timestamp);
        }

        @NotNull
        static List<ChatMessage> fromOverloaded(@NotNull List<PlainChatMessage> messages) {
            List<ChatMessage> list = new ArrayList<>(messages.size());
            for (PlainChatMessage msg : messages) list.add(fromOverloaded(msg));
            return list;
        }

        @NotNull
        static ChatMessage fromOverloaded(@NotNull PlainChatMessage msg) {
            return new ChatMessage(msg.from, msg.text, msg.timestamp);
        }
    }

    @UiThread
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private static final int TYPE_SENT = 0;
        private static final int TYPE_RECEIVED = 1;
        private final List<ChatMessage> list;
        private final boolean showSender;

        ChatAdapter(@NotNull List<ChatMessage> list, boolean showSender) {
            this.list = new LinkedList<>(list);
            this.showSender = showSender;

            onUpdatedItemCount(list.size());
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage msg = list.get(position);
            if (controller != null && msg.sender.equals(controller.username()))
                return TYPE_SENT;
            else
                return TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage msg = list.get(position);

            holder.setText(msg.text);
            if (showSender) holder.setSender(msg.sender);
            else holder.setSender(null);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void addNewMessage(@NotNull ChatMessage msg) {
            list.add(msg);
            notifyItemInserted(list.size() - 1);
            onUpdatedItemCount(list.size());
        }

        public void addOlderMessages(List<ChatMessage> messages) {
            list.addAll(0, messages);
            notifyItemRangeInserted(0, messages.size());
            onUpdatedItemCount(list.size());
        }

        public long olderTimestamp() {
            return list.isEmpty() ? 0 : list.get(0).timestamp;
        }

        private void onUpdatedItemCount(int count) {
            binding.chatFragmentListEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            binding.chatFragmentList.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView text;
            private final TextView sender;

            ViewHolder(@NonNull ViewGroup parent, int viewType) {
                super(getLayoutInflater().inflate(viewType == TYPE_RECEIVED ? R.layout.item_new_chat_msg_received : R.layout.item_new_chat_msg_sent, parent, false));

                text = itemView.findViewById(R.id.chatItem_text);
                sender = itemView.findViewById(R.id.chatItem_sender);
            }

            void setText(String text) {
                this.text.setText(text);
            }

            void setSender(String sender) {
                if (this.sender != null) {
                    this.sender.setText(sender);
                    if (sender == null) this.sender.setVisibility(View.GONE);
                    else this.sender.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
