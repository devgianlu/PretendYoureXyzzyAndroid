package com.gianlu.pretendyourexyzzy.metrics;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.CardsGridFixer;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameHistory;

public class GameHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<GameHistory> {
    private static final String TAG = GameHistoryFragment.class.getSimpleName();
    private RecyclerMessageView rmv;

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
        rmv = new RecyclerMessageView(requireContext());
        rmv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rmv.startLoading();
        rmv.list().addOnLayoutChangeListener(new CardsGridFixer(requireContext()));

        Bundle args = getArguments();
        String id;
        GameHistory history = null;
        if (args == null || ((id = args.getString("id", null)) == null
                && (history = (GameHistory) args.getSerializable("history")) == null)) {
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        if (history == null) {
            RegisteredPyx pyx;
            try {
                pyx = RegisteredPyx.get();
            } catch (LevelMismatchException ex) {
                rmv.showError(R.string.failedLoading);
                return rmv;
            }

            pyx.getGameHistory(id, null, this);
        } else {
            onDone(history);
        }

        return rmv;
    }

    @Override
    public void onDone(@NonNull GameHistory result) {
        if (getContext() == null) return;

        rmv.loadListData(new RoundsAdapter(getContext(), result, (RoundsAdapter.Listener) getContext()));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading history.", ex);
        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }
}
