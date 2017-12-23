package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.NamesAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.FirestoreHelper;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NamesFragment extends Fragment implements PYX.IResult<List<String>> {
    private final FirestoreHelper firestore = FirestoreHelper.getInstance();
    private RecyclerViewLayout layout;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = new RecyclerViewLayout(inflater);
        layout.enableSwipeRefresh(R.color.colorAccent);
        layout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        final PYX pyx = PYX.get(getContext());

        layout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pyx.getNamesList(NamesFragment.this);
            }
        });

        pyx.getNamesList(this);

        return layout;
    }

    public void scrollToTop() {
        layout.getList().scrollToPosition(0);
    }

    @Override
    public void onDone(PYX pyx, final List<String> result) {
        if (!isAdded()) return;

        firestore.getMobileNicknames().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                loadListData(result, snapshot.getDocuments());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception ex) {
                loadListData(result, new ArrayList<DocumentSnapshot>());
                Logging.logMe(ex);
            }
        });

        names = result.size();
        updateActivityTitle();
    }

    private void loadListData(List<String> result, List<DocumentSnapshot> mobiles) {
        layout.loadListData(new NamesAdapter(getContext(), result, mobiles));
    }

    @Override
    public void onException(Exception ex) {
        Logging.logMe(ex);
        if (isAdded())
            layout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
    }
}
