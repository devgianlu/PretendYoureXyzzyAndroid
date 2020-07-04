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

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.adapters.ChatAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.dialogs.UserInfoDialog;

public class ChatFragment extends FragmentWithDialog implements ChatAdapter.Listener, Pyx.OnEventListener {
    private static final String TAG = ChatFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private ChatAdapter adapter;
    private int gid;
    private RegisteredPyx pyx;
    private ImageButton send;
    private EditText message;

    @NonNull
    public static ChatFragment getGameInstance(int gid) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putInt("gid", gid);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static ChatFragment getGlobalInstance() {
        return new ChatFragment();
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

        Bundle args = getArguments();
        if (args == null) gid = -1;
        else gid = args.getInt("gid", -1);

        adapter = new ChatAdapter(requireContext(), this);
        this.rmv.loadListData(adapter);
        onItemCountChanged(0);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            this.rmv.showError(R.string.failedLoading);
            return layout;
        }

        message = layout.findViewById(R.id.chatFragment_message);
        send = layout.findViewById(R.id.chatFragment_send);
        send.setOnClickListener(v -> {
            String msg = message.getText().toString();
            if (msg.isEmpty()) return;
            sendMessage(msg);
        });

        pyx.polling().addListener(this);

        return layout;
    }

    private void sendMessage(@NonNull String msg) {
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
                    showToast(Toaster.build().message(R.string.unknownChatCommand));
                    return;
            }
        } else {
            emote = false;
            wall = false;
        }

        message.setEnabled(false);
        send.setEnabled(false);
        send(msg, emote, wall, new Pyx.OnSuccess() {
            @Override
            public void onDone() {
                message.setText(null);
                message.setEnabled(true);
                send.setEnabled(true);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed sending message.", ex);
                showToast(Toaster.build().message(R.string.failedSendMessage));
                message.setEnabled(true);
                send.setEnabled(true);
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (pyx != null) pyx.polling().removeListener(this);
        super.onDestroy();
    }

    private void send(@NonNull String msg, boolean emote, boolean wall, @NonNull Pyx.OnSuccess listener) {
        if (gid == -1) {
            pyx.request(PyxRequests.sendMessage(msg, emote, wall), getActivity(), listener);
            AnalyticsApplication.sendAnalytics(Utils.ACTION_SENT_MSG);
        } else {
            pyx.request(PyxRequests.sendGameMessage(gid, msg, emote), getActivity(), listener);
            AnalyticsApplication.sendAnalytics(Utils.ACTION_SENT_GAME_MSG);
        }
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
        if (activity == null) return;

        UserInfoDialog.loadAndShow(pyx, activity, sender);
    }

    @Override
    public void onPollMessage(@NonNull PollMessage message) {
        if (!isAdded()) return;
        adapter.newMessage(message, gid);
        rmv.list().scrollToPosition(adapter.getItemCount() - 1);
    }
}

