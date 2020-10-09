package com.gianlu.pretendyourexyzzy.metrics;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;

public class UserHistoryFragment extends FragmentWithDialog {
    private static final String TAG = UserHistoryFragment.class.getSimpleName();
    private RegisteredPyx pyx;
    private RecyclerMessageView rmv;

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
                .addOnSuccessListener(requireActivity(), result -> {
                    rmv.loadListData(new UserHistoryAdapter(requireContext(), pyx, result, (UserHistoryAdapter.Listener) getContext()), false);
                    if (result.isEmpty()) rmv.showInfo(R.string.noMetricsSessions);
                    else rmv.showList();
                })
                .addOnFailureListener(requireActivity(), ex -> {
                    Log.e(TAG, "Failed getting history.", ex);
                    rmv.showError(R.string.failedLoading_reason, ex.getMessage());
                });
        return rmv;
    }
}
