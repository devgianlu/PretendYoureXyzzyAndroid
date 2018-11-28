package com.gianlu.pretendyourexyzzy.Main;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.pretendyourexyzzy.Adapters.NamesAdapter;
import com.gianlu.pretendyourexyzzy.Dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Name;
import com.gianlu.pretendyourexyzzy.NetIO.Models.PollMessage;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxRequests;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONException;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NamesFragment extends Fragment implements Pyx.OnResult<List<Name>>, NamesAdapter.Listener, MenuItem.OnActionExpandListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {
    private RecyclerViewLayout layout;
    private int names = -1;
    private RegisteredPyx pyx;
    private NamesAdapter adapter;
    private SearchView searchView;

    @NonNull
    public static NamesFragment getInstance() {
        NamesFragment fragment = new NamesFragment();
        fragment.setHasOptionsMenu(true);
        return fragment;
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.names_fragment, menu);

        if (getContext() == null) return;
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.namesFragment_search);
        item.setOnActionExpandListener(this);

        if (searchManager != null && getActivity() != null) {
            searchView = (SearchView) item.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        onQueryTextSubmit(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (adapter != null) adapter.filterWithQuery(query);
        return true;
    }

    @Override
    public boolean onClose() {
        searchView.setQuery(null, true);
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = new RecyclerViewLayout(requireContext());
        layout.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Logging.log(ex);
            layout.showError(R.string.failedLoading);
            return layout;
        }

        layout.enableSwipeRefresh(() -> pyx.request(PyxRequests.getNamesList(), NamesFragment.this), R.color.colorAccent);

        pyx.polling().addListener(new Pyx.OnEventListener() {
            @Override
            public void onPollMessage(@NonNull PollMessage msg) throws JSONException {
                switch (msg.event) {
                    case NEW_PLAYER:
                        if (adapter != null)
                            adapter.itemChangedOrAdded(new Name(msg.obj.getString("n")));
                        break;
                    case PLAYER_LEAVE:
                        if (adapter != null) adapter.removeItem(msg.obj.getString("n"));
                        break;
                    case GAME_LIST_REFRESH:
                        pyx.request(PyxRequests.getNamesList(), NamesFragment.this);
                        break;
                }
            }

            @Override
            public void onStoppedPolling() {
            }
        });

        pyx.request(PyxRequests.getNamesList(), this);

        return layout;
    }

    public void scrollToTop() {
        if (layout != null) layout.getList().scrollToPosition(0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDone(@NonNull final List<Name> result) {
        if (!isAdded()) return;

        adapter = new NamesAdapter(getContext(), result, this);
        layout.loadListData(adapter);

        names = result.size();
        updateActivityTitle();
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        layout.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Override
    public void onNameSelected(@NonNull String name) {
        final FragmentActivity activity = getActivity();
        if (activity != null) UserInfoDialog.loadAndShow(pyx, activity, name);
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) layout.showInfo(R.string.noNames);
        else layout.showList();
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onClose();
        return true;
    }
}
