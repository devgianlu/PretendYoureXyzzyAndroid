package com.gianlu.pretendyourexyzzy.main.chats;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.google.android.material.textfield.TextInputLayout;

public class PyxChatFragment extends FragmentWithDialog implements ChatAdapter.Listener, PyxChatController.Listener {
    private static final String TAG = PyxChatFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private ChatAdapter adapter;
    private TextInputLayout input;
    private PyxChatController controller;

    @NonNull
    public static PyxChatFragment getGameInstance(int gid, @NonNull Context context) {
        PyxChatFragment fragment = new PyxChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("type", Type.GAME);
        args.putString("title", context.getString(R.string.gameChat));
        args.putInt("gid", gid);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static PyxChatFragment getGlobalInstance(@NonNull Context context) {
        PyxChatFragment fragment = new PyxChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("type", Type.GLOBAL);
        args.putString("title", context.getString(R.string.globalChat));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (controller != null) controller.readAllMessages();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_pyx_chat, container, false);
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
                controller = PyxChatController.globalController();
                break;
            case GAME:
                controller = PyxChatController.gameController(args.getInt("gid", -1));
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
        } catch (LevelMismatchException ex) {
            this.rmv.showError(R.string.failedLoading);
            return layout;
        }

        input = layout.findViewById(R.id.chatFragment_input);
        input.setEndIconOnClickListener(v -> {
            String msg = CommonUtils.getText(input);
            if (msg.isEmpty() || controller == null) return;

            input.setEnabled(false);
            controller.send(msg, getActivity(), new PyxChatController.SendCallback() {
                @Override
                public void onSuccessful() {
                    CommonUtils.setText(input, null);
                    input.setEnabled(true);
                }

                @Override
                public void unknownCommand() {
                    showToast(Toaster.build().message(R.string.unknownChatCommand));
                    input.setEnabled(true);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed sending message.", ex);
                    showToast(Toaster.build().message(R.string.failedSendMessage));
                    input.setEnabled(true);
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
    public void onChatMessage(@NonNull PollMessage msg) {
        adapter.newMessage(msg);
        if (isAdded()) rmv.list().scrollToPosition(adapter.getItemCount() - 1);
    }

    public void onSelectedFragment() {
        if (controller != null) controller.readAllMessages();
    }

    private enum Type {
        GLOBAL, GAME
    }
}

