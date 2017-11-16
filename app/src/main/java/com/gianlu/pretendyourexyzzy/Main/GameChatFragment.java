package com.gianlu.pretendyourexyzzy.Main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gianlu.commonutils.AnalyticsApplication;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.ChatAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;

import org.json.JSONException;

public class GameChatFragment extends Fragment implements ChatAdapter.IAdapter, PYX.IEventListener {
    private static final String POLL_TAG = "gameChat";
    private RecyclerViewLayout recyclerViewLayout;
    private ChatAdapter adapter;
    private Game game;

    public static GameChatFragment getInstance(Game game) {
        GameChatFragment fragment = new GameChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("game", game);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.chat_fragment, container, false);
        recyclerViewLayout = layout.findViewById(R.id.chatFragment_recyclerViewLayout);
        recyclerViewLayout.disableSwipeRefresh();
        LinearLayoutManager llm = new SuppressingLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        llm.setStackFromEnd(true);
        recyclerViewLayout.setLayoutManager(llm);
        recyclerViewLayout.getList().addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        game = (Game) getArguments().getSerializable("game");
        if (game == null) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        adapter = new ChatAdapter(getContext(), this);
        recyclerViewLayout.loadListData(adapter);

        final PYX pyx = PYX.get(getContext());

        final EditText message = layout.findViewById(R.id.chatFragment_message);
        ImageButton send = layout.findViewById(R.id.chatFragment_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                if (msg.isEmpty() || game == null) return;

                pyx.sendGameMessage(game.gid, msg, new PYX.ISuccess() {
                    @Override
                    public void onDone(PYX pyx) {
                        message.setText(null);

                        AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_SENT_GAME_MSG);
                    }

                    @Override
                    public void onException(Exception ex) {
                        Toaster.show(getActivity(), Utils.Messages.FAILED_SEND_MESSAGE, ex);
                    }
                });
            }
        });

        pyx.getPollingThread().addListener(POLL_TAG, this);

        return layout;
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
    public void onPollMessage(PollMessage message) throws JSONException {
        if (game == null || !isAdded()) return;
        adapter.newMessage(message, game);
    }

    @Override
    public void onStoppedPolling() {
    }
}

