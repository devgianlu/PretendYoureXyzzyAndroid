package com.gianlu.pretendyourexyzzy.Main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.ChatAdapter;
import com.gianlu.pretendyourexyzzy.Dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

public class ChatFragment extends Fragment implements ChatAdapter.Listener, Pyx.OnEventListener {
    private RecyclerViewLayout recyclerViewLayout;
    private ChatAdapter adapter;
    private int gid;
    private RegisteredPyx pyx;

    @NonNull
    public static ChatFragment getGameInstance(int gid) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("gid", gid);
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
        if (getContext() == null) return layout;
        recyclerViewLayout = layout.findViewById(R.id.chatFragment_recyclerViewLayout);
        recyclerViewLayout.disableSwipeRefresh();
        LinearLayoutManager llm = new SuppressingLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        llm.setStackFromEnd(true);
        recyclerViewLayout.setLayoutManager(llm);

        Bundle args = getArguments();
        if (args == null) gid = -1;
        else gid = args.getInt("gid", -1);

        adapter = new ChatAdapter(getContext(), this);
        recyclerViewLayout.loadListData(adapter);
        onItemCountChanged(0);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        final EditText message = layout.findViewById(R.id.chatFragment_message);
        final ImageButton send = layout.findViewById(R.id.chatFragment_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                if (msg.isEmpty()) return;

                message.setEnabled(false);
                send.setEnabled(false);
                send(msg, new Pyx.OnSuccess() {
                    @Override
                    public void onDone() {
                        message.setText(null);
                        message.setEnabled(true);
                        send.setEnabled(true);
                    }

                    @Override
                    public void onException(@NonNull Exception ex) {
                        Toaster.show(getActivity(), Utils.Messages.FAILED_SEND_MESSAGE, ex);
                        message.setEnabled(true);
                        send.setEnabled(true);
                    }
                });
            }
        });

        pyx.polling().addListener(ChatFragment.class.getName() + gid, this);

        return layout;
    }

    @Override
    public void onDestroy() {
        if (pyx != null) pyx.polling().removeListener(ChatFragment.class.getName() + gid);
        super.onDestroy();
    }

    private void send(String msg, Pyx.OnSuccess listener) {
        if (gid == -1) {
            pyx.request(PyxRequests.sendMessage(msg), listener);
            AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_SENT_MSG);
        } else {
            pyx.request(PyxRequests.sendGameMessage(gid, msg), listener);
            AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_SENT_GAME_MSG);
        }
    }

    public void scrollToTop() {
        recyclerViewLayout.getList().scrollToPosition(0);
    }

    @Override
    public void onItemCountChanged(int count) {
        if (count == 0) recyclerViewLayout.showMessage(R.string.noMessages, false);
        else recyclerViewLayout.showList();
    }

    @Override
    public void onChatItemSelected(@NonNull String sender) {
        FragmentActivity activity = getActivity();
        if (activity == null) return;

        UserInfoDialog.loadAndShow(pyx, activity, sender);
    }

    @Override
    public void onPollMessage(PollMessage message) {
        if (!isAdded()) return;
        adapter.newMessage(message, gid);
        recyclerViewLayout.getList().scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void onStoppedPolling() {
    }
}

