package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.ChatAdapter;
import com.gianlu.pretendyourexyzzy.main.chats.ChatController;
import com.gianlu.pretendyourexyzzy.main.chats.PyxChat;

public class ChatFragment extends FragmentWithDialog implements ChatAdapter.Listener, ChatController.Listener {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private ChatAdapter adapter;
    private ImageButton send;
    private EditText message;
    private ChatController controller;

    @NonNull
    public static ChatFragment getGameInstance(int gid) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("type", Type.GAME);
        args.putInt("gid", gid);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static ChatFragment getGlobalInstance() {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("type", Type.GLOBAL);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_chat, container, false);
        this.rmv = layout.findViewById(R.id.chatFragment_recyclerViewLayout);
        this.rmv.disableSwipeRefresh();
        LinearLayoutManager llm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        llm.setStackFromEnd(true);
        rmv.setLayoutManager(llm);

        Type type;
        Bundle args = getArguments();
        if (args == null || (type = (Type) args.getSerializable("type")) == null) {
            this.rmv.showError(R.string.failedLoading);
            return layout;
        }

        switch (type) {
            case GLOBAL:
                controller = PyxChat.globalController();
                break;
            case GAME:
                controller = PyxChat.gameController(args.getInt("gid", -1));
                break;
            default:
                this.rmv.showError(R.string.failedLoading);
                return layout;
        }

        adapter = new ChatAdapter(requireContext(), this);
        this.rmv.loadListData(adapter);
        onItemCountChanged(0);

        try {
            controller.init();
        } catch (ChatController.InitException ex) {
            this.rmv.showError(R.string.failedLoading);
            return layout;
        }

        message = layout.findViewById(R.id.chatFragment_message);
        send = layout.findViewById(R.id.chatFragment_send);
        send.setOnClickListener(v -> {
            String msg = message.getText().toString();
            if (msg.isEmpty() || controller == null) return;

            send.setEnabled(false);
            message.setEnabled(false);
            controller.send(msg, getActivity(), new ChatController.SendCallback() {
                @Override
                public void onSuccessful() {
                    message.setText(null);
                    send.setEnabled(true);
                    message.setEnabled(true);
                }

                @Override
                public void unknownCommand() {
                    showToast(Toaster.build().message(R.string.unknownChatCommand));
                    send.setEnabled(true);
                    message.setEnabled(true);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed sending message.", ex);
                    showToast(Toaster.build().message(R.string.failedSendMessage));
                    send.setEnabled(true);
                    message.setEnabled(true);
                }
            });
        });

        controller.listener(this);

        return layout;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (controller != null) controller.onDestroy();
        super.onDestroy();
    }

    public void scrollToTop() {
        if (rmv != null) rmv.list().scrollToPosition(0);
    }

    @Override
    public void onItemCountChanged(int count) {
        if (count == 0) rmv.showInfo(R.string.noMessages);
        else rmv.showList();
    }

    @Override
    public void onChatItemSelected(@NonNull String sender) {
        FragmentActivity activity = getActivity();
        if (activity != null) controller.showUserInfo(activity, sender);
    }

    @Override
    public void onChatMessage(@NonNull ChatController.ChatMessage msg) {
        if (!isAdded()) return;
        adapter.newMessage(msg);
        rmv.list().scrollToPosition(adapter.getItemCount() - 1);
    }

    private enum Type {
        GLOBAL, GAME
    }
}

