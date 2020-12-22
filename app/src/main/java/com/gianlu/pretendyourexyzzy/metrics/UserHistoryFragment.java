package com.gianlu.pretendyourexyzzy.metrics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionStats;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class UserHistoryFragment extends FragmentWithDialog {
    private static final String TAG = UserHistoryFragment.class.getSimpleName();
    private RegisteredPyx pyx;
    private RecyclerMessageView rmv;
    private Listener listener;

    @NonNull
    public static UserHistoryFragment get() {
        return new UserHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rmv = new RecyclerMessageView(requireContext());
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        rmv.startLoading();

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        pyx.getUserHistory()
                .addOnSuccessListener(result -> {
                    rmv.loadListData(new UserHistoryAdapter(result), false);
                    if (result.isEmpty()) rmv.showInfo(R.string.noMetricsSessions);
                    else rmv.showList();
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed getting history.", ex);
                    rmv.showError(R.string.failedLoading_reason, ex.getMessage());
                });
        return rmv;
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

    public interface Listener {
        void onSessionSelected(@NonNull UserHistory.Session session);
    }

    private class UserHistoryAdapter extends RecyclerView.Adapter<UserHistoryAdapter.ViewHolder> {
        private final UserHistory history;

        UserHistoryAdapter(@NonNull UserHistory history) {
            this.history = history;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.size() == 0) {
                onBindViewHolder(holder, position);
            } else {
                Object payload = payloads.get(0);
                if (payload instanceof SessionStats)
                    holder.playedAndJudged.setHtml(R.string.playedAndJudged, ((SessionStats) payload).playedRounds, ((SessionStats) payload).judgedRounds);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserHistory.Session item = history.get(position);
            holder.server.setText(item.name());
            holder.login.setText(CommonUtils.getFullVerbalDateFormatter().format(new Date(item.loginTimestamp)));
            holder.playedAndJudged.setText(null);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSessionSelected(item);
            });

            pyx.getSessionStats(item.id)
                    .addOnSuccessListener(result -> notifyItemChanged(holder.getAdapterPosition(), result))
                    .addOnFailureListener(ex -> Log.e(TAG, "Failed getting session stats.", ex));
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final TextView server;
            final TextView login;
            final SuperTextView playedAndJudged;

            public ViewHolder(@NotNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_metrics_session, parent, false));

                server = itemView.findViewById(R.id.itemSession_server);
                login = itemView.findViewById(R.id.itemSession_login);
                playedAndJudged = itemView.findViewById(R.id.itemSession_playedAndJudged);
            }
        }
    }
}
