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
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameHistory;

public class GameHistoryFragment extends FragmentWithDialog {
    private static final String TAG = GameHistoryFragment.class.getSimpleName();
    private RecyclerMessageView rmv;
    private RoundsAdapter.Listener listener;

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
        rmv.setLayoutManager(new GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false));
        rmv.startLoading();

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

            pyx.getGameHistory(id)
                    .addOnSuccessListener(requireActivity(), result -> rmv.loadListData(new RoundsAdapter(requireContext(), result, listener)))
                    .addOnFailureListener(requireActivity(), ex -> {
                        Log.e(TAG, "Failed loading history.", ex);
                        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
                    });
        } else {
            rmv.loadListData(new RoundsAdapter(requireContext(), history, listener));
        }

        return rmv;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof RoundsAdapter.Listener)
            listener = (RoundsAdapter.Listener) context;
        if (getParentFragment() instanceof RoundsAdapter.Listener)
            listener = (RoundsAdapter.Listener) getParentFragment();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
