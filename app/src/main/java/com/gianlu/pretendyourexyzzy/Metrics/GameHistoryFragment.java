package com.gianlu.pretendyourexyzzy.Metrics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.Adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.GameHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

public class GameHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<GameHistory> {
    private RecyclerMessageView layout;

    @NonNull
    public static GameHistoryFragment get(@NonNull String id) {
        GameHistoryFragment fragment = new GameHistoryFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static GameHistoryFragment get(@NonNull GameHistory history) {
        GameHistoryFragment fragment = new GameHistoryFragment();
        Bundle args = new Bundle();
        args.putSerializable("history", history);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = new RecyclerMessageView(requireContext());
        layout.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        layout.startLoading();
        layout.list().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        Bundle args = getArguments();
        String id;
        GameHistory history = null;
        if (args == null || ((id = args.getString("id", null)) == null
                && (history = (GameHistory) args.getSerializable("history")) == null)) {
            layout.showError(R.string.failedLoading);
            return layout;
        }

        if (history == null) {
            RegisteredPyx pyx;
            try {
                pyx = RegisteredPyx.get();
            } catch (LevelMismatchException ex) {
                Logging.log(ex);
                layout.showError(R.string.failedLoading);
                return layout;
            }

            pyx.getGameHistory(id, null, this);
        } else {
            onDone(history);
        }

        return layout;
    }

    @Override
    public void onDone(@NonNull GameHistory result) {
        if (getContext() == null) return;

        layout.loadListData(new RoundsAdapter(getContext(), result, (RoundsAdapter.Listener) getContext()));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        layout.showError(R.string.failedLoading_reason, ex.getMessage());
    }
}
