package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.ServersChecker;

import java.util.List;
import java.util.Locale;

public class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ViewHolder> implements ServersChecker.OnResult {
    private final LayoutInflater inflater;
    private final List<Pyx.Server> servers;
    private final ServersChecker checker;
    private final Listener listener;

    public ServersAdapter(Context context, List<Pyx.Server> servers, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.servers = servers;
        this.listener = listener;
        this.checker = new ServersChecker();

        listener.shouldUpdateItemCount(getItemCount());
        startTests();
    }

    public void startTests() {
        for (Pyx.Server server : servers) checker.check(server, this);
    }

    public void removeItem(Pyx.Server server) {
        int index = servers.indexOf(server);
        if (index != -1) {
            servers.remove(index);
            notifyItemRemoved(index);
            listener.shouldUpdateItemCount(getItemCount());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Pyx.Server server = servers.get(position);
        holder.name.setText(server.name);
        holder.url.setText(server.url.toString());

        holder.hasMetrics.setVisibility(server.hasMetrics() ? View.VISIBLE : View.GONE);

        if (server.status == null) {
            holder.checking.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
            holder.error.setVisibility(View.GONE);
            holder.details.setVisibility(View.GONE);
        } else {
            holder.checking.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);
            switch (server.status.status) {
                case ONLINE:
                    holder.statusIcon.setImageResource(R.drawable.baseline_done_24);
                    holder.latency.setVisibility(View.VISIBLE);
                    holder.latency.setText(String.format(Locale.getDefault(), "%dms", server.status.latency));
                    holder.error.setVisibility(View.GONE);

                    ServersChecker.CheckResult.Stats stats = server.status.stats;
                    holder.details.setVisibility(View.VISIBLE);
                    holder.details.setHtml(R.string.usersAndGames, stats.users(), stats.maxUsers(), stats.games(), stats.maxGames());

                    holder.hasGameChat.setVisibility(stats.gameChatEnabled() ? View.VISIBLE : View.GONE);
                    holder.hasChat.setVisibility(stats.globalChatEnabled() ? View.VISIBLE : View.GONE);
                    holder.hasBlankCards.setVisibility(stats.blankCardsEnabled() ? View.VISIBLE : View.GONE);
                    break;
                case ERROR:
                    holder.statusIcon.setImageResource(R.drawable.baseline_error_outline_24);
                    holder.latency.setVisibility(View.GONE);
                    holder.details.setVisibility(View.GONE);

                    holder.error.setVisibility(View.VISIBLE);
                    holder.error.setText(server.status.ex.getLocalizedMessage());
                    break;
                case OFFLINE:
                    holder.statusIcon.setImageResource(R.drawable.baseline_clear_24);
                    holder.latency.setVisibility(View.GONE);
                    holder.details.setVisibility(View.GONE);

                    holder.error.setVisibility(View.VISIBLE);
                    holder.error.setText(server.status.ex.getLocalizedMessage());
                    break;
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.serverSelected(server);
        });
    }

    @Override
    public int getItemCount() {
        return servers.size();
    }

    @Override
    public void serverChecked(@NonNull Pyx.Server server) {
        int index = servers.indexOf(server);
        if (index != -1) notifyItemChanged(index);
    }

    public interface Listener {
        void shouldUpdateItemCount(int count);

        void serverSelected(@NonNull Pyx.Server server);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView url;
        final ProgressBar checking;
        final LinearLayout status;
        final ImageView statusIcon;
        final TextView latency;
        final TextView error;
        final SuperTextView details;
        final ImageView hasMetrics;
        final ImageView hasChat;
        final ImageView hasGameChat;
        final ImageView hasBlankCards;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_server, parent, false));

            name = itemView.findViewById(R.id.serverItem_name);
            url = itemView.findViewById(R.id.serverItem_url);
            checking = itemView.findViewById(R.id.serverItem_checking);
            status = itemView.findViewById(R.id.serverItem_status);
            statusIcon = status.findViewById(R.id.serverItem_statusIcon);
            latency = status.findViewById(R.id.serverItem_latency);
            error = itemView.findViewById(R.id.serverItem_error);
            details = itemView.findViewById(R.id.serverItem_details);
            hasChat = itemView.findViewById(R.id.serverItem_hasChat);
            hasGameChat = itemView.findViewById(R.id.serverItem_hasGameChat);
            hasMetrics = itemView.findViewById(R.id.serverItem_hasMetrics);
            hasBlankCards = itemView.findViewById(R.id.serverItem_hasBlankCards);
        }
    }
}
