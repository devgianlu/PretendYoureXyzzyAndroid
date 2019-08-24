package com.gianlu.pretendyourexyzzy.Metrics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CasualViews.RecyclerMessageView;
import com.gianlu.commonutils.Dialogs.FragmentWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

public class UserHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<UserHistory> {
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
            Logging.log(ex);
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        pyx.getUserHistory(null, this);

        return rmv;
    }

    @Override
    public void onDone(@NonNull UserHistory result) {
        if (getContext() == null) return;

        rmv.loadListData(new UserHistoryAdapter(getContext(), pyx, result, (UserHistoryAdapter.Listener) getContext()));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }
}
