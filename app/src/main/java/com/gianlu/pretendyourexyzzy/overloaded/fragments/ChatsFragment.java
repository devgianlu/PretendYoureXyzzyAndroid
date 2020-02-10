package com.gianlu.pretendyourexyzzy.overloaded.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.overloaded.ChatsAdapter;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedApi;

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

        OverloadedApi.ChatModule chat = OverloadedApi.get().chat();
        if (chat == null) {
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        chat.getAllChats()
                .addOnSuccessListener(requireActivity(), chats -> {
                    rmv.loadListData(new ChatsAdapter(requireContext(), chat, chats));
                    if (chats.isEmpty()) rmv.showInfo(R.string.overloaded_noChats);
                })
                .addOnFailureListener(requireActivity(), ex -> {
                    rmv.showError(R.string.failedLoading);
                    Logging.log(ex);
                });

        return rmv;
    }
}
