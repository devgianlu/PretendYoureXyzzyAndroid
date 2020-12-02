package com.gianlu.pretendyourexyzzy.metrics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class SessionHistoryFragment extends FragmentWithDialog {
    private static final String TAG = SessionHistoryFragment.class.getSimpleName();
    private ProgressBar loading;
    private SuperTextView gamesLabel;
    private RecyclerView games;
    private SuperTextView playedRoundsLabel;
    private RecyclerView playedRounds;
    private SuperTextView judgedRoundsLabel;
    private RecyclerView judgedRounds;
    private LinearLayout container;
    private MessageView message;
    private Listener listener;

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
            loading.setVisibility(View.GONE);
            message.error(R.string.failedLoading);
            return layout;
        }

        pyx.getSessionHistory(id)
                .addOnSuccessListener(result -> {
                    if (result.games.isEmpty() && result.judgedRounds.isEmpty() && result.playedRounds.isEmpty()) {
                        loading.setVisibility(View.GONE);
                        container.setVisibility(View.GONE);
                        message.info(R.string.noActivity);
                    } else {
                        loading.setVisibility(View.GONE);
                        container.setVisibility(View.VISIBLE);
                        gamesLabel.setHtml(R.string.gamesCount, result.games.size());
                        games.setAdapter(new GamesAdapter(result.games));
                        playedRoundsLabel.setHtml(R.string.playedRoundsCount, result.playedRounds.size());
                        playedRounds.setAdapter(new RoundsAdapter(requireContext(), result.playedRounds, listener));
                        judgedRoundsLabel.setHtml(R.string.judgedRoundsCount, result.judgedRounds.size());
                        judgedRounds.setAdapter(new RoundsAdapter(requireContext(), result.judgedRounds, listener));
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed loading history.", ex);
                    loading.setVisibility(View.GONE);
                    container.setVisibility(View.GONE);
                    message.error(R.string.failedLoading_reason, ex.getMessage());
                });

        return layout;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener)
            listener = (Listener) context;
        if (getParentFragment() instanceof Listener)
            listener = (Listener) getParentFragment();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface Listener extends RoundsAdapter.Listener {
        void onGameSelected(@NonNull SessionHistory.Game game);
    }

    private class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
        private final List<SessionHistory.Game> games;

        GamesAdapter(List<SessionHistory.Game> games) {
            this.games = games;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SessionHistory.Game game = games.get(position);
            ((SuperTextView) holder.itemView).setHtml(R.string.gameStartedAt, CommonUtils.getFullVerbalDateFormatter().format(new Date(game.timestamp)));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onGameSelected(game);
            });
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NotNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_metrics_game, parent, false));
            }
        }
    }
}
