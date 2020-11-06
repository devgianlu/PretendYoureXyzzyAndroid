package com.gianlu.pretendyourexyzzy.main;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BlockedUsers;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.Name;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewPlayersSettingsBinding;
import com.gianlu.pretendyourexyzzy.dialogs.NewUserInfoDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public class NewPlayersFragment extends NewSettingsFragment.ChildFragment implements Pyx.OnEventListener, OverloadedApi.EventListener {
    private static final String TAG = NewPlayersFragment.class.getSimpleName();
    private FragmentNewPlayersSettingsBinding binding;
    private RegisteredPyx pyx;
    private PlayersAdapter adapter;
    private Task<List<String>> overloadedUsersTask;
    private List<String> overloadedUsers;

    private void setPlayersStatus(boolean loading, boolean error) {
        if (loading) {
            binding.playersFragmentList.setAdapter(null);
            binding.playersFragmentListError.setVisibility(View.GONE);
            binding.playersFragmentListLoading.setVisibility(View.VISIBLE);
            binding.playersFragmentListLoading.showShimmer(true);

            binding.playersFragmentSwipeRefresh.setEnabled(false);
        } else if (error) {
            binding.playersFragmentList.setAdapter(null);
            binding.playersFragmentListError.setVisibility(View.VISIBLE);
            binding.playersFragmentListLoading.setVisibility(View.GONE);
            binding.playersFragmentListLoading.hideShimmer();

            binding.playersFragmentSwipeRefresh.setEnabled(true);
        } else {
            binding.playersFragmentListError.setVisibility(View.GONE);
            binding.playersFragmentListLoading.setVisibility(View.GONE);
            binding.playersFragmentListLoading.hideShimmer();

            binding.playersFragmentSwipeRefresh.setEnabled(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewPlayersSettingsBinding.inflate(getLayoutInflater(), container, false);
        binding.playersFragmentBack.setOnClickListener(v -> goBack());

        binding.playersFragmentList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        binding.playersFragmentSwipeRefresh.setColorSchemeResources(R.color.appColor_500);
        binding.playersFragmentSwipeRefresh.setOnRefreshListener(() -> {
            if (pyx == null) return;

            setPlayersStatus(true, false);
            pyx.request(PyxRequests.getNamesList())
                    .addOnSuccessListener(this::playersLoaded)
                    .addOnFailureListener(this::failedLoadingPlayers);

            binding.playersFragmentSwipeRefresh.setRefreshing(false);
        });

        Utils.generateUsernamePlaceholders(requireContext(), binding.playersFragmentListLoadingChild, 14, 12, 40);
        setPlayersStatus(true, false);

        if (OverloadedUtils.isSignedIn()) {
            OverloadedApi.get().addEventListener(this);
            loadOverloadedUsers();
        }

        return binding.getRoot();
    }

    @NonNull
    private Task<List<String>> loadOverloadedUsers() {
        if (!OverloadedUtils.isSignedIn())
            return overloadedUsersTask = Tasks.forException(new Exception("Overloaded not signed in."));

        if (overloadedUsersTask != null && !overloadedUsersTask.isComplete())
            return overloadedUsersTask;

        if (pyx == null)
            return overloadedUsersTask = Tasks.forException(new Exception("Missing Pyx instance."));

        return overloadedUsersTask = OverloadedApi.get().listUsersOnServer(pyx.server.url)
                .addOnSuccessListener(list -> overloadedUsers = list)
                .addOnFailureListener(ex -> Log.e(TAG, "Failed getting Overloaded users.", ex));
    }

    private void playersLoaded(@NonNull List<Name> names) {
        adapter = new PlayersAdapter(names);
        binding.playersFragmentList.setAdapter(adapter);
        setPlayersStatus(false, false);
    }

    private void failedLoadingPlayers(@NonNull Exception ex) {
        Log.e(TAG, "Failed loading players names.", ex);
        setPlayersStatus(false, true);
    }

    @Override
    protected void onPyxReady(@NonNull RegisteredPyx pyx) {
        this.pyx = pyx;
        this.pyx.polling().addListener(this);

        loadOverloadedUsers().continueWithTask(task -> pyx.request(PyxRequests.getNamesList()))
                .addOnSuccessListener(this::playersLoaded)
                .addOnFailureListener(this::failedLoadingPlayers);

        binding.playersFragmentSwipeRefresh.setEnabled(true);
    }

    @Override
    protected void onPyxInvalid() {
        if (pyx != null) pyx.polling().removeListener(this);
        this.pyx = null;

        if (binding != null) {
            setPlayersStatus(true, false);
            binding.playersFragmentSwipeRefresh.setEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OverloadedApi.get().removeEventListener(this);
        if (pyx != null) pyx.polling().removeListener(this);
        this.pyx = null;
    }

    @Override
    public void onPollMessage(@NonNull PollMessage msg) throws JSONException {
        switch (msg.event) {
            case NEW_PLAYER:
                if (adapter != null)
                    adapter.itemChangedOrAdded(new Name(msg.obj.getString("n")));
                break;
            case PLAYER_LEAVE:
                if (adapter != null) {
                    String username = msg.obj.getString("n");
                    adapter.removeItem((item) -> item.noSigil().equals(username));
                }
                break;
            case GAME_LIST_REFRESH:
                loadOverloadedUsers().continueWithTask(task -> pyx.request(PyxRequests.getNamesList()))
                        .addOnSuccessListener((names) -> {
                            if (adapter != null) adapter.itemsChanged(names);
                        })
                        .addOnFailureListener((ex) -> Log.e(TAG, "Failed refreshing names list.", ex));
                break;
        }
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.data == null) return;

        if (event.type == OverloadedApi.Event.Type.USER_LEFT_SERVER) {
            String username = event.data.getString("nick");
            if (overloadedUsers != null)
                if (overloadedUsers.remove(username) && adapter != null)
                    adapter.itemChanged((item) -> item.noSigil().equals(username));
        } else if (event.type == OverloadedApi.Event.Type.USER_JOINED_SERVER) {
            String username = event.data.getString("nick");
            if (overloadedUsers != null)
                if (overloadedUsers.add(username) && adapter != null)
                    adapter.itemChanged((item) -> item.noSigil().equals(username));
        }
    }

    public enum Sorting {
        AZ, ZA
    }

    private class PlayersAdapter extends OrderedRecyclerViewAdapter<PlayersAdapter.ViewHolder, Name, Sorting, Void> {

        PlayersAdapter(@NonNull List<Name> list) {
            super(list, Sorting.AZ);
        }

        @Override
        protected boolean matchQuery(@NonNull Name item, @Nullable String query) {
            return true;
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name name) {
            ((SuperTextView) holder.itemView).setHtml(name.sigil() == Name.Sigil.NORMAL_USER ? name.withSigil() : (SuperTextView.makeBold(name.sigil().symbol()) + name.noSigil()));
            holder.itemView.setOnClickListener(v -> {
                if (name.noSigil().equals(Utils.myPyxUsername())) {
                    showToast(Toaster.build().message(R.string.thisIsYou));
                    return;
                }

                showPopup(holder.itemView.getContext(), holder.itemView, name.noSigil());
            });

            if (hasOverloadedUser(name.noSigil()))
                CommonUtils.setTextColor((TextView) holder.itemView, R.color.appColor_500);
            else
                CommonUtils.setTextColorFromAttr((TextView) holder.itemView, android.R.attr.textColorSecondary);
        }

        private boolean hasOverloadedUser(@NonNull String username) {
            return overloadedUsers != null && overloadedUsers.contains(username);
        }

        private void showPopup(@NonNull Context context, @NonNull View anchor, @NonNull String username) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.inflate(R.menu.item_name);

            Menu menu = popup.getMenu();
            if (BlockedUsers.isBlocked(username)) {
                menu.removeItem(R.id.nameItemMenu_block);
                menu.removeItem(R.id.nameItemMenu_addFriend);
            } else {
                menu.removeItem(R.id.nameItemMenu_unblock);
                if (hasOverloadedUser(username)) {
                    Map<String, FriendStatus> map = OverloadedApi.get().friendsStatusCache();
                    if (map != null && map.containsKey(username))
                        menu.removeItem(R.id.nameItemMenu_addFriend);
                } else {
                    menu.removeItem(R.id.nameItemMenu_addFriend);
                }
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.nameItemMenu_showInfo:
                        NewUserInfoDialog.get(username, true, hasOverloadedUser(username)).show(getChildFragmentManager(), null);
                        return true;
                    case R.id.nameItemMenu_unblock:
                        BlockedUsers.unblock(username);
                        return true;
                    case R.id.nameItemMenu_block:
                        BlockedUsers.block(username);
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

            CommonUtils.showPopupOffsetDip(context, popup, 24, -8);
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Name payload) {
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
        }

        @NonNull
        @Override
        public Comparator<Name> getComparatorFor(@NonNull Sorting sorting) {
            switch (sorting) {
                default:
                case AZ:
                    return new Name.AzComparator();
                case ZA:
                    return new Name.ZaComparator();
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final SuperTextView text;

            ViewHolder(@NonNull ViewGroup parent) {
                super(NewPlayersFragment.this.getLayoutInflater().inflate(R.layout.item_name, parent, false));
                text = (SuperTextView) itemView;
            }
        }
    }
}
