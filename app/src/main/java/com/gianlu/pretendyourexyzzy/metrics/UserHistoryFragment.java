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
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;

public class UserHistoryFragment extends FragmentWithDialog implements Pyx.OnResult<UserHistory> {
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
        Log.e(TAG, "Failed getting history.", ex);
        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }
}
