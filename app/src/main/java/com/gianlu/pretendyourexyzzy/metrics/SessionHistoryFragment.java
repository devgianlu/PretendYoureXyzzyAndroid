package com.gianlu.pretendyourexyzzy.metrics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;

public class SessionHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<SessionHistory> {
    private ProgressBar loading;
    private SuperTextView gamesLabel;
    private RecyclerView games;
    private SuperTextView playedRoundsLabel;
    private RecyclerView playedRounds;
    private SuperTextView judgedRoundsLabel;
    private RecyclerView judgedRounds;
    private LinearLayout container;
    private MessageView message;

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
        NestedScrollView layout = (NestedScrollView) inflater.inflate(R.layout.fragment_metrics_session, parent, false);

        message = layout.findViewById(R.id.sessionFragment_message);
        container = layout.findViewById(R.id.sessionFragment_container);
        gamesLabel = container.findViewById(R.id.sessionFragment_gamesLabel);
        games = container.findViewById(R.id.sessionFragment_games);
        games.setNestedScrollingEnabled(false);
        games.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        playedRoundsLabel = container.findViewById(R.id.sessionFragment_playedRoundsLabel);
        playedRounds = container.findViewById(R.id.sessionFragment_playedRounds);
        playedRounds.setNestedScrollingEnabled(false);
        playedRounds.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        playedRounds.addOnLayoutChangeListener(new CardsGridFixer(requireContext()));
        judgedRoundsLabel = container.findViewById(R.id.sessionFragment_judgedRoundsLabel);
        judgedRounds = container.findViewById(R.id.sessionFragment_judgedRounds);
        judgedRounds.setNestedScrollingEnabled(false);
        judgedRounds.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        judgedRounds.addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        loading = layout.findViewById(R.id.sessionFragment_loading);
        loading.setVisibility(View.VISIBLE);
        container.setVisibility(View.GONE);

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            loading.setVisibility(View.GONE);
            message.error(R.string.failedLoading);
            return layout;
        }

        Pyx pyx;
        try {
            pyx = Pyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            loading.setVisibility(View.GONE);
            message.error(R.string.failedLoading);
            return layout;
        }

        pyx.getSessionHistory(id, null, this);

        return layout;
    }

    @Override
    public void onDone(@NonNull SessionHistory result) {
        if (getContext() == null) return;

        if (result.games.isEmpty()) {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
            message.info(R.string.noActivity);
        } else {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);
            gamesLabel.setHtml(R.string.gamesCount, result.games.size());
            games.setAdapter(new GamesAdapter(getContext(), result.games, (GamesAdapter.Listener) getContext()));
            playedRoundsLabel.setHtml(R.string.playedRoundsCount, result.playedRounds.size());
            playedRounds.setAdapter(new RoundsAdapter(getContext(), result.playedRounds, (RoundsAdapter.Listener) getContext()));
            judgedRoundsLabel.setHtml(R.string.judgedRoundsCount, result.judgedRounds.size());
            judgedRounds.setAdapter(new RoundsAdapter(getContext(), result.judgedRounds, (RoundsAdapter.Listener) getContext()));
        }
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.GONE);
        message.error(R.string.failedLoading_reason, ex.getMessage());
    }
}
