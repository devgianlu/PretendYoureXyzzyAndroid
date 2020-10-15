package com.gianlu.pretendyourexyzzy.main;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BlockedUsers;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Name;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.dialogs.UserInfoDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUserProfileBottomSheet;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public class NamesFragment extends FragmentWithDialog implements MenuItem.OnActionExpandListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener, OverloadedApi.EventListener, Pyx.OnEventListener {
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
            rmv.showError(R.string.failedLoading);
            return rmv;
        }

        rmv.enableSwipeRefresh(() -> {
            adapter = null;
            pyx.request(PyxRequests.getNamesList())
                    .addOnSuccessListener(this::loadedNames)
                    .addOnFailureListener(this::failedLoadingNames);
        }, R.color.colorAccent);

        pyx.polling().addListener(this);
        pyx.request(PyxRequests.getNamesList())
                .addOnSuccessListener(this::loadedNames)
                .addOnFailureListener(this::failedLoadingNames);

        if (OverloadedUtils.isSignedIn()) {
            OverloadedApi.get().addEventListener(this);
            OverloadedApi.get().listUsers(pyx.server.url)
                    .addOnSuccessListener(requireActivity(), list -> {
                        overloadedUsers.clear();
                        overloadedUsers.addAll(list);
                        if (adapter != null) adapter.setOverloadedUsers(list);
                    })
                    .addOnFailureListener(ex -> {
                        overloadedUsers.clear();
                        Log.e(TAG, "Failed listing Overloaded users.", ex);
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

    private void loadedNames(@NonNull List<Name> result) {
        adapter = new NamesAdapter(getContext(), result, overloadedUsers);
        rmv.loadListData(adapter);

        if (searchView != null && searchView.getQuery() != null)
            onQueryTextSubmit(searchView.getQuery().toString());

        names = result.size();
        updateActivityTitle();
    }

    private void failedLoadingNames(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading names.", ex);
        if (!PyxException.solveNotRegistered(getContext(), ex))
            rmv.showError(R.string.failedLoading_reason, ex.getMessage());
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
                pyx.request(PyxRequests.getNamesList())
                        .addOnSuccessListener(this::loadedNames)
                        .addOnFailureListener(this::failedLoadingNames);
                break;
        }
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (adapter == null || event.data == null) return;

        if (event.type == OverloadedApi.Event.Type.USER_LEFT_SERVER)
            adapter.overloadedUserLeft(event.data.getString("nick"));
        else if (event.type == OverloadedApi.Event.Type.USER_JOINED_SERVER)
            adapter.overloadedUserJoined(event.data.getString("nick"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OverloadedApi.get().removeEventListener(this);
        if (pyx != null) pyx.polling().removeListener(this);
    }

    public enum Sorting {
        AZ,
        ZA
    }

    private class NamesAdapter extends OrderedRecyclerViewAdapter<NamesAdapter.ViewHolder, Name, Sorting, Void> {
        private final LayoutInflater inflater;
        private final List<String> overloadedUsers;

        NamesAdapter(Context context, List<Name> names, @NonNull List<String> overloadedUsers) {
            super(names, Sorting.AZ);
            this.inflater = LayoutInflater.from(context);
            this.overloadedUsers = overloadedUsers;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        protected boolean matchQuery(@NonNull Name item, @Nullable String query) {
            return query == null || item.withSigil().toLowerCase().contains(query.toLowerCase());
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name name) {
            ((SuperTextView) holder.itemView).setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));
            holder.itemView.setOnClickListener(v -> showPopup(holder.itemView.getContext(), holder.itemView, name.noSigil()));
            if (overloadedUsers.contains(name.noSigil()))
                CommonUtils.setTextColor((TextView) holder.itemView, R.color.appColor_500);
            else
                CommonUtils.setTextColorFromAttr((TextView) holder.itemView, android.R.attr.textColorSecondary);
        }

        private void showPopup(@NonNull Context context, @NonNull View anchor, @NonNull String username) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.inflate(R.menu.item_name);

            Menu menu = popup.getMenu();
            if (!username.equals(Utils.myPyxUsername())) {
                if (BlockedUsers.isBlocked(username)) {
                    menu.removeItem(R.id.nameItemMenu_block);
                    menu.removeItem(R.id.nameItemMenu_showProfile);
                    menu.removeItem(R.id.nameItemMenu_addFriend);
                } else {
                    menu.removeItem(R.id.nameItemMenu_unblock);
                    if (overloadedUsers.contains(username)) {
                        Map<String, FriendStatus> map = OverloadedApi.get().friendsStatusCache();
                        if (map != null && map.containsKey(username))
                            menu.removeItem(R.id.nameItemMenu_addFriend);
                    } else {
                        menu.removeItem(R.id.nameItemMenu_showProfile);
                        menu.removeItem(R.id.nameItemMenu_addFriend);
                    }
                }
            } else {
                menu.removeItem(R.id.nameItemMenu_unblock);
                menu.removeItem(R.id.nameItemMenu_block);
                menu.removeItem(R.id.nameItemMenu_addFriend);
                menu.removeItem(R.id.nameItemMenu_showProfile);
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.nameItemMenu_showInfo:
                        FragmentActivity activity = getActivity();
                        if (activity != null) UserInfoDialog.loadAndShow(pyx, activity, username);
                        return true;
                    case R.id.nameItemMenu_unblock:
                        BlockedUsers.unblock(username);
                        return true;
                    case R.id.nameItemMenu_block:
                        BlockedUsers.block(username);
                        return true;
                    case R.id.nameItemMenu_showProfile:
                        OverloadedUserProfileBottomSheet.get().show(NamesFragment.this, username);
                        return true;
                    case R.id.nameItemMenu_addFriend:
                        OverloadedApi.get().addFriend(username)
                                .addOnSuccessListener(map -> {
                                    AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_ADD_FRIEND);
                                    showToast(Toaster.build().message(R.string.friendAdded).extra(username));
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed adding friend.", ex);
                                    showToast(Toaster.build().message(R.string.failedAddingFriend).extra(username));
                                });
                        return true;
                    default:
                        return false;
                }
            });

            CommonUtils.showPopupOffset(popup, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics()), 0);
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name payload) {
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            if (count == 0) rmv.showInfo(R.string.noNames);
            else rmv.showList();
        }

        @NonNull
        @Override
        public Comparator<Name> getComparatorFor(@NonNull @NotNull Sorting sorting) {
            switch (sorting) {
                default:
                case AZ:
                    return new Name.AzComparator();
                case ZA:
                    return new Name.ZaComparator();
            }
        }

        void removeItem(String name) {
            Iterator<Name> iter = originalObjs.iterator();
            while (iter.hasNext()) {
                if (name.equals(iter.next().noSigil())) {
                    iter.remove();
                    break;
                }
            }
        }

        void overloadedUserLeft(@NonNull String nick) {
            if (!overloadedUsers.remove(nick)) return;

            for (Name name : originalObjs) {
                if (name.noSigil().equals(nick)) {
                    itemChangedOrAdded(name);
                    break;
                }
            }
        }

        void overloadedUserJoined(@NonNull String nick) {
            if (!overloadedUsers.add(nick)) return;

            for (Name name : originalObjs) {
                if (name.noSigil().equals(nick)) {
                    itemChangedOrAdded(name);
                    break;
                }
            }
        }

        void setOverloadedUsers(@NonNull List<String> list) {
            overloadedUsers.clear();
            overloadedUsers.addAll(list);
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(ViewGroup parent) {
                super(inflater.inflate(R.layout.item_name, parent, false));
            }
        }
    }
}
