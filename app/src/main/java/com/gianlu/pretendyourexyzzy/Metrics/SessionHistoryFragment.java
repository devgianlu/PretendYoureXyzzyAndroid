package com.gianlu.pretendyourexyzzy.Metrics;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardsGridLayoutFixer;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.R;

public class SessionHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<SessionHistory>, SwipeRefreshLayout.OnRefreshListener {
    private Pyx pyx;
    private ProgressBar loading;
    private SwipeRefreshLayout layout;
    private RecyclerView games;
    private RecyclerView playedRounds;
    private RecyclerView judgedRounds;
    private LinearLayout container;

    @NonNull
    public static SessionHistoryFragment get(@NonNull String id) {
        SessionHistoryFragment fragment = new SessionHistoryFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_metrics_session, parent, false);
        layout.setColorSchemeResources(R.color.colorAccent);
        layout.setOnRefreshListener(this);

        container = layout.findViewById(R.id.sessionFragment_container);
        games = layout.findViewById(R.id.sessionFragment_games);
        games.setNestedScrollingEnabled(false);
        games.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        playedRounds = layout.findViewById(R.id.sessionFragment_playedRounds);
        playedRounds.setNestedScrollingEnabled(false);
        playedRounds.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        playedRounds.addOnLayoutChangeListener(new CardsGridLayoutFixer());
        judgedRounds = layout.findViewById(R.id.sessionFragment_judgedRounds);
        judgedRounds.setNestedScrollingEnabled(false);
        judgedRounds.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        judgedRounds.addOnLayoutChangeListener(new CardsGridLayoutFixer());

        loading = layout.findViewById(R.id.sessionFragment_loading);
        loading.setVisibility(View.VISIBLE);
        container.setVisibility(View.GONE);

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            loading.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        try {
            pyx = Pyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            loading.setVisibility(View.GONE);
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        pyx.getSessionHistory(id, this);

        return layout;
    }

    @Override
    public void onDone(@NonNull SessionHistory result) {
        loading.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        games.setAdapter(new GamesAdapter(getContext(), result.games, (GamesAdapter.Listener) getContext()));
        playedRounds.setAdapter(new RoundsAdapter(getContext(), result.playedRounds, (RoundsAdapter.Listener) getContext()));
        judgedRounds.setAdapter(new RoundsAdapter(getContext(), result.judgedRounds, (RoundsAdapter.Listener) getContext()));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        loading.setVisibility(View.GONE);
        MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Override
    public void onRefresh() {
        // TODO
    }
}
