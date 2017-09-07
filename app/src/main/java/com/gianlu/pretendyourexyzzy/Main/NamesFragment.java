package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.pretendyourexyzzy.Adapters.NamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;

import java.util.List;

public class NamesFragment extends Fragment implements PYX.IResult<List<String>> {
    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar loading;
    private FrameLayout layout;
    private int names = -1;

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (FrameLayout) inflater.inflate(R.layout.recycler_view_layout, container, false);
        loading = layout.findViewById(R.id.recyclerViewLayout_loading);
        swipeRefresh = layout.findViewById(R.id.recyclerViewLayout_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent);
        list = layout.findViewById(R.id.recyclerViewLayout_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        final PYX pyx = PYX.get(getContext());

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pyx.getNamesList(NamesFragment.this);
            }
        });

        pyx.getNamesList(this);

        return layout;
    }

    public void scrollToTop() {
        if (list != null) list.scrollToPosition(0);
    }

    @Override
    public void onDone(PYX pyx, List<String> result) {
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        MessageLayout.hide(layout);

        if (isAdded()) {
            list.setAdapter(new NamesAdapter(getContext(), result));
            names = result.size();
            updateActivityTitle();
        }
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(getContext(), ex);
        swipeRefresh.setRefreshing(false);
        loading.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.GONE);
        if (isAdded())
            MessageLayout.show(layout, getString(R.string.failedLoading_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }
}
