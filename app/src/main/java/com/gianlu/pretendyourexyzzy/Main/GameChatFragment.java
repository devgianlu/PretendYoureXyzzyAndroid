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
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.ThisApplication;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.android.gms.analytics.HitBuilders;

import java.util.List;

public class GameChatFragment extends Fragment implements PYX.IResult<List<PollMessage>>, ChatAdapter.IAdapter {
    private static final String POLL_TAG = "gameChat";
    private RecyclerView list;
    private LinearLayout layout;
    private Game game;
    private ChatAdapter adapter;

    public static GameChatFragment getInstance(Game game) {
        GameChatFragment fragment = new GameChatFragment();
        Bundle args = new Bundle();
        args.putSerializable("game", game);
        fragment.setArguments(args);
        return fragment;
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

        game = (Game) getArguments().getSerializable("game");
        if (game == null) {
            loading.setVisibility(View.GONE);
            swipeRefresh.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        adapter = new ChatAdapter(getContext(), this);
        list.setAdapter(adapter);

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

                        ThisApplication.sendAnalytics(getContext(), new HitBuilders.EventBuilder()
                                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                                .setAction(ThisApplication.ACTION_SENT_GAME_MSG)
                                .build());
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
        if (list != null) list.scrollToPosition(0);
    }

    @Override
    public void onDone(PYX pyx, List<PollMessage> result) {
        if (game == null) return;
        adapter.newMessages(result, game);
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

