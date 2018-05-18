package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.NamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Name;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.UserInfoDialog;

import java.util.List;

public class NamesFragment extends Fragment implements Pyx.OnResult<List<Name>>, NamesAdapter.Listener {
    private RecyclerViewLayout layout;
    private int names = -1;
    private RegisteredPyx pyx;

    public static NamesFragment getInstance() {
        return new NamesFragment();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) updateActivityTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityTitle();
    }

    private void updateActivityTitle() {
        Activity activity = getActivity();
        if (names != -1 && activity != null && isVisible())
            activity.setTitle(getString(R.string.playersLabel) + " (" + names + ") - " + getString(R.string.app_name));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = new RecyclerViewLayout(inflater);
        layout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            layout.showMessage(R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return layout;
        }

        layout.enableSwipeRefresh(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pyx.request(PyxRequests.getNamesList(), NamesFragment.this);
            }
        }, R.color.colorAccent);

        pyx.request(PyxRequests.getNamesList(), this);

        return layout;
    }

    public void scrollToTop() {
        layout.getList().scrollToPosition(0);
    }

    @Override
    public void onDone(@NonNull final List<Name> result) {
        if (!isAdded()) return;

        layout.loadListData(new NamesAdapter(getContext(), result, this));

        names = result.size();
        updateActivityTitle();
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        if (isAdded())
            layout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
    }

    @Override
    public void onNameSelected(@NonNull String name) {
        final FragmentActivity activity = getActivity();
        if (activity != null) UserInfoDialog.loadAndShow(pyx, activity, name);
    }
}
