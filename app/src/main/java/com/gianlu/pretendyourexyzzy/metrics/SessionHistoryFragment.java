package com.gianlu.pretendyourexyzzy.metrics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.databinding.FragmentMetricsSessionBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class SessionHistoryFragment extends FragmentWithDialog {
    private static final String TAG = SessionHistoryFragment.class.getSimpleName();
    private FragmentMetricsSessionBinding binding;
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
        binding = FragmentMetricsSessionBinding.inflate(inflater, parent, false);

        binding.sessionFragmentGames.setNestedScrollingEnabled(false);
        binding.sessionFragmentGames.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        binding.sessionFragmentPlayedRounds.setNestedScrollingEnabled(false);
        binding.sessionFragmentPlayedRounds.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false));

        binding.sessionFragmentJudgedRounds.setNestedScrollingEnabled(false);
        binding.sessionFragmentJudgedRounds.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false));

        binding.sessionFragmentContainer.setVisibility(View.GONE);
        binding.sessionFragmentEmpty.setVisibility(View.GONE);
        binding.sessionFragmentError.setVisibility(View.GONE);
        binding.sessionFragmentLoading.setVisibility(View.VISIBLE);

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            onBackPressed();
            return null;
        }

        Pyx pyx;
        try {
            pyx = Pyx.get();
        } catch (LevelMismatchException ex) {
            onBackPressed();
            return null;
        }

        pyx.getSessionHistory(id)
                .addOnSuccessListener(result -> {
                    if (result.games.isEmpty() && result.judgedRounds.isEmpty() && result.playedRounds.isEmpty()) {
                        binding.sessionFragmentLoading.setVisibility(View.GONE);
                        binding.sessionFragmentContainer.setVisibility(View.GONE);
                        binding.sessionFragmentError.setVisibility(View.GONE);
                        binding.sessionFragmentEmpty.setVisibility(View.VISIBLE);
                    } else {
                        binding.sessionFragmentEmpty.setVisibility(View.GONE);
                        binding.sessionFragmentLoading.setVisibility(View.GONE);
                        binding.sessionFragmentError.setVisibility(View.GONE);
                        binding.sessionFragmentContainer.setVisibility(View.VISIBLE);
                        binding.sessionFragmentGamesLabel.setHtml(R.string.gamesCount, result.games.size());
                        binding.sessionFragmentGames.setAdapter(new GamesAdapter(result.games));
                        binding.sessionFragmentPlayedRoundsLabel.setHtml(R.string.playedRoundsCount, result.playedRounds.size());
                        binding.sessionFragmentPlayedRounds.setAdapter(new RoundsAdapter(requireContext(), result.playedRounds, listener));
                        binding.sessionFragmentJudgedRoundsLabel.setHtml(R.string.judgedRoundsCount, result.judgedRounds.size());
                        binding.sessionFragmentJudgedRounds.setAdapter(new RoundsAdapter(requireContext(), result.judgedRounds, listener));
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed loading history.", ex);
                    binding.sessionFragmentLoading.setVisibility(View.GONE);
                    binding.sessionFragmentContainer.setVisibility(View.GONE);
                    binding.sessionFragmentEmpty.setVisibility(View.GONE);
                    binding.sessionFragmentError.setVisibility(View.VISIBLE);
                });

        return binding.getRoot();
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
