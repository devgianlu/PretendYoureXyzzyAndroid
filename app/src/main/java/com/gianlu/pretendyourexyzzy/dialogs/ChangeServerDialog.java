package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.ServersChecker;
import com.gianlu.pretendyourexyzzy.databinding.DialogChangeServerBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemNewServerBinding;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class ChangeServerDialog extends DialogFragment {
    private ServersAdapter adapter;
    private NewMainActivity parent;

    @NotNull
    public static ChangeServerDialog get() {
        return new ChangeServerDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        Window window;
        if (dialog != null && (window = dialog.getWindow()) != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DialogChangeServerBinding binding = DialogChangeServerBinding.inflate(inflater, container, false);
        binding.changeServerDialogCancel.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.changeServerDialogList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        adapter = new ServersAdapter(Pyx.Server.loadAllServers());
        binding.changeServerDialogList.setAdapter(adapter);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter != null) adapter.startTests();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NewMainActivity)
            parent = (NewMainActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parent = null;
    }

    private class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ViewHolder> implements ServersChecker.OnResult {
        private final List<Pyx.Server> servers;
        private final ServersChecker checker;

        ServersAdapter(@NotNull List<Pyx.Server> servers) {
            this.servers = servers;
            this.checker = new ServersChecker();
        }

        public void startTests() {
            for (Pyx.Server server : servers)
                checker.check(server, this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Pyx.Server server = servers.get(position);
            holder.binding.serverItemName.setText(server.name);
            holder.binding.serverItemFeatures.setFeatures(server);

            if (server.status == null) {
                holder.binding.serverItemFeaturesLoading.showShimmer(true);
                holder.binding.serverItemStatus.setVisibility(View.GONE);
                holder.binding.serverItemUsers.setVisibility(View.GONE);
                holder.binding.serverItemGames.setVisibility(View.GONE);
            } else {
                holder.binding.serverItemFeaturesLoading.hideShimmer();
                holder.binding.serverItemStatus.setVisibility(View.VISIBLE);

                ServersChecker.CheckResult.Stats stats = server.status.stats;
                switch (server.status.status) {
                    case ONLINE:
                        holder.binding.serverItemUsers.setVisibility(View.VISIBLE);
                        holder.binding.serverItemUsers.setHtml(String.format(Locale.getDefault(), "%s: <b>%d</b>/%d", getString(R.string.users), stats.users(), stats.maxUsers()));
                        holder.binding.serverItemGames.setVisibility(View.VISIBLE);
                        holder.binding.serverItemGames.setHtml(String.format(Locale.getDefault(), "%s: <b>%d</b>/%d", getString(R.string.games), stats.games(), stats.maxGames()));

                        holder.binding.serverItemStatus.setTextColor(Color.rgb(60, 148, 6));
                        holder.binding.serverItemStatus.setText(String.format(Locale.getDefault(), "%dms", server.status.latency));
                        break;
                    case ERROR:
                        holder.binding.serverItemUsers.setVisibility(View.GONE);
                        holder.binding.serverItemGames.setVisibility(View.GONE);

                        CommonUtils.setTextColor(holder.binding.serverItemStatus, R.color.red);
                        holder.binding.serverItemStatus.setText(getString(R.string.error).toLowerCase());
                        break;
                    case OFFLINE:
                        holder.binding.serverItemUsers.setVisibility(View.GONE);
                        holder.binding.serverItemGames.setVisibility(View.GONE);

                        CommonUtils.setTextColor(holder.binding.serverItemStatus, R.color.red);
                        holder.binding.serverItemStatus.setText(getString(R.string.offline).toLowerCase());
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (parent != null) {
                    parent.changeServer(server);
                    dismissAllowingStateLoss();
                }
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

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemNewServerBinding binding;

            ViewHolder(@Nullable ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_new_server, parent, false));
                binding = ItemNewServerBinding.bind(itemView);
            }
        }
    }
}
