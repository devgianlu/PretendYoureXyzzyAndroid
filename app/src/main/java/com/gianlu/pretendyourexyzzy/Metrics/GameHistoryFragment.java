package com.gianlu.pretendyourexyzzy.Metrics;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.GameHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

public class GameHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<GameHistory> {
    private RecyclerViewLayout layout;

    @NonNull
    public static GameHistoryFragment get(@NonNull String id) {
        GameHistoryFragment fragment = new GameHistoryFragment();
        Bundle args = new Bundle();
        args.putString("id", id);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = new RecyclerViewLayout(inflater);
        layout.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        layout.startLoading();
        layout.getList().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        Bundle args = getArguments();
        String id;
        if (args == null || (id = args.getString("id", null)) == null) {
            layout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        RegisteredPyx pyx;
        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            layout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        pyx.getGameHistory(id, this);

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
        layout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
    }
}
