package com.gianlu.pretendyourexyzzy.Main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.ChatAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import java.util.List;

public class GlobalChatFragment extends Fragment implements PYX.IResult<List<PollMessage>>, ChatAdapter.IAdapter {
    private static final String POLL_TAG = "globalChat";
    private RecyclerView list;
    private LinearLayout layout;
    private ChatAdapter adapter;

    public static GlobalChatFragment getInstance() {
        return new GlobalChatFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.chat_fragment, container, false);
        ProgressBar loading = layout.findViewById(R.id.recyclerViewLayout_loading);
        SwipeRefreshLayout swipeRefresh = layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setEnabled(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        list = layout.findViewById(R.id.recyclerViewLayout_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        llm.setStackFromEnd(true);
        list.setLayoutManager(llm);
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        adapter = new ChatAdapter(getContext(), this);
        list.setAdapter(adapter);

        final PYX pyx = PYX.get(getContext());

        final EditText message = layout.findViewById(R.id.chatFragment_message);
        ImageButton send = layout.findViewById(R.id.chatFragment_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                if (msg.isEmpty()) return;

                pyx.sendMessage(msg, new PYX.ISuccess() {
                    @Override
                    public void onDone(PYX pyx) {
                        message.setText(null);
                    }

                    @Override
                    public void onException(Exception ex) {
                        Toaster.show(getActivity(), Utils.Messages.FAILED_SEND_MESSAGE, ex);
                    }
                });
            }
        });

        pyx.pollingThread.addListener(POLL_TAG, this);

        return layout;
    }

    public void scrollToTop() {
        if (list != null) list.scrollToPosition(0);
    }

    @Override
    public void onDone(PYX pyx, List<PollMessage> result) {
        adapter.newMessages(result, null);
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
    }

    @Override
    public void onItemCountChanged(int count) {
        if (count == 0)
            MessageLayout.show(layout, R.string.noMessages, R.drawable.ic_info_outline_black_48dp);
        else
            MessageLayout.hide(layout);
    }
}
