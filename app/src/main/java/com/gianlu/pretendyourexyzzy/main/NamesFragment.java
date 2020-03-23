package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.NamesAdapter;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Name;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.UsersCallback;

public class NamesFragment extends FragmentWithDialog implements Pyx.OnResult<List<Name>>, NamesAdapter.Listener, MenuItem.OnActionExpandListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener, OverloadedApi.EventListener {
    private static final String TAG = NamesFragment.class.getSimpleName();
    private final List<String> overloadedUsers = new ArrayList<>();
    private RecyclerMessageView rmv;
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
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.names_fragment, menu);

        SearchManager searchManager = (SearchManager) requireContext().getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.namesFragment_search);
        item.setOnActionExpandListener(this);

        if (searchManager != null && getActivity() != null) {
            searchView = (SearchView) item.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);

            TextView textView = searchView.findViewById(getResources().getIdentifier("android:id/search_src_text", null, null));
            if (textView != null) CommonUtils.setTextColorFromAttr(textView, R.attr.colorOnPrimary);
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
        rmv = new RecyclerMessageView(requireContext());
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Log.e(TAG, "LevelMismatchException", ex);
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        rmv.enableSwipeRefresh(() -> {
            adapter = null;
            pyx.request(PyxRequests.getNamesList(), null, NamesFragment.this);
        }, R.color.colorAccent);

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
                        adapter = null;
                        pyx.request(PyxRequests.getNamesList(), null, NamesFragment.this);
                        break;
                }
            }

            @Override
            public void onStoppedPolling() {
            }
        });

        pyx.request(PyxRequests.getNamesList(), null, this);
        if (OverloadedUtils.isSignedIn()) {
            OverloadedApi.get().addEventListener(this);
            OverloadedApi.get().listUsers(pyx.server.url, getActivity(), new UsersCallback() {
                @Override
                public void onUsers(@NonNull List<String> list) {
                    overloadedUsers.clear();
                    overloadedUsers.addAll(list);
                    if (adapter != null) adapter.setOverloadedUsers(list);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    overloadedUsers.clear();
                    Log.e(TAG, "Failed listing Overloaded users.", ex);
                }
            });
        }
        return rmv;
    }

    public void scrollToTop() {
        if (rmv != null) rmv.list().scrollToPosition(0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDone(@NonNull List<Name> result) {
        if (!isAdded()) return;

        adapter = new NamesAdapter(getContext(), result, overloadedUsers, this);
        rmv.loadListData(adapter);

        if (searchView != null && searchView.getQuery() != null)
            onQueryTextSubmit(searchView.getQuery().toString());

        names = result.size();
        updateActivityTitle();
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading names.", ex);
        if (!PyxException.solveNotRegistered(getContext(), ex))
            rmv.showError(R.string.failedLoading_reason, ex.getMessage());
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) rmv.showInfo(R.string.noNames);
        else rmv.showList();
    }

    @Override
    public void onShowUserInfo(@NonNull String name) {
        FragmentActivity activity = getActivity();
        if (activity != null) UserInfoDialog.loadAndShow(pyx, activity, name);
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

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (adapter == null) return;

        if (event.type == OverloadedApi.Event.Type.USER_LEFT_SERVER)
            adapter.overloadedUserLeft(event.obj.getString("nick"));
        else if (event.type == OverloadedApi.Event.Type.USER_JOINED_SERVER)
            adapter.overloadedUserJoined(event.obj.getString("nick"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OverloadedApi.get().removeEventListener(this);
    }
}
