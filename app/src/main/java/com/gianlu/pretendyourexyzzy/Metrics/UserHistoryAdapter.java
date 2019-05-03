package com.gianlu.pretendyourexyzzy.Metrics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CasualViews.SuperTextView;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.SessionStats;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Date;
import java.util.List;

class UserHistoryAdapter extends RecyclerView.Adapter<UserHistoryAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final UserHistory history;
    private final Listener listener;
    private final Pyx pyx;

    UserHistoryAdapter(@NonNull Context context, @NonNull Pyx pyx, @NonNull UserHistory history, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.pyx = pyx;
        this.history = history;
        this.listener = listener;
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
            if (payload instanceof SessionStats) {
                holder.playedAndJudged.setHtml(R.string.playedAndJudged, ((SessionStats) payload).playedRounds, ((SessionStats) payload).judgedRounds);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final UserHistory.Session item = history.get(position);
        holder.server.setText(item.name());
        holder.login.setText(CommonUtils.getFullVerbalDateFormatter().format(new Date(item.loginTimestamp)));
        holder.playedAndJudged.setText(null);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionSelected(item);
        });

        pyx.getSessionStats(item.id, null, new Pyx.OnResult<SessionStats>() {
            @Override
            public void onDone(@NonNull SessionStats result) {
                notifyItemChanged(holder.getAdapterPosition(), result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Logging.log(ex);
            }
        });
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    public interface Listener {
        void onSessionSelected(@NonNull UserHistory.Session session);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView server;
        final TextView login;
        final SuperTextView playedAndJudged;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_metrics_session, parent, false));

            server = itemView.findViewById(R.id.itemSession_server);
            login = itemView.findViewById(R.id.itemSession_login);
            playedAndJudged = itemView.findViewById(R.id.itemSession_playedAndJudged);
        }
    }
}
