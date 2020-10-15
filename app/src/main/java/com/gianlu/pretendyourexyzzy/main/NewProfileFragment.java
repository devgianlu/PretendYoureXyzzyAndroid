package com.gianlu.pretendyourexyzzy.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.NewMainActivity;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewProfileBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemFriendBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemStarredCardBinding;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase.StarredCard;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public class NewProfileFragment extends FragmentWithDialog implements NewMainActivity.MainFragment, OverloadedApi.EventListener {
    private static final String TAG = NewProfileFragment.class.getSimpleName();
    private FragmentNewProfileBinding binding;
    private RegisteredPyx pyx;
    private FriendsAdapter friendsAdapter;

    @NonNull
    public static NewProfileFragment get() {
        return new NewProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewProfileBinding.inflate(inflater, container, false);
        binding.profileFragmentInputs.idCodeInput.setEndIconOnClickListener(v -> CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, CommonUtils.randomString(100)));

        OverloadedApi.get().addEventListener(this);

        //region Starred cards
        binding.profileFragmentStarredCardsList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        StarredCardsDatabase starredDb = StarredCardsDatabase.get(requireContext());
        List<StarredCard> starredCards = starredDb.getCards(false);
        if (starredCards.isEmpty()) {
            binding.profileFragmentStarredCardsEmpty.setVisibility(View.VISIBLE);
            binding.profileFragmentStarredCardsList.setVisibility(View.GONE);
        } else {
            binding.profileFragmentStarredCardsEmpty.setVisibility(View.GONE);
            binding.profileFragmentStarredCardsList.setVisibility(View.VISIBLE);
            binding.profileFragmentStarredCardsList.setAdapter(new StarredCardsAdapter(starredDb, starredCards));
        }
        //endregion

        //region Friends
        binding.profileFragmentFriendsList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.profileFragmentFriendsLoading.setVisibility(View.VISIBLE);
        OverloadedUtils.waitReady().addOnSuccessListener(signedIn -> {
            if (signedIn) {
                binding.profileFragmentFriendsOverloaded.setVisibility(View.GONE);

                OverloadedApi.get().friendsStatus()
                        .addOnSuccessListener(friends -> {
                            binding.profileFragmentFriendsLoading.setVisibility(View.GONE);

                            friendsAdapter = new FriendsAdapter(friends.values());
                            binding.profileFragmentFriendsList.setAdapter(friendsAdapter);
                        })
                        .addOnFailureListener(ex -> {
                            Log.e(TAG, "Failed loading friends.", ex);
                            binding.profileFragmentFriendsLoading.setVisibility(View.GONE);
                            // TODO: Show error
                        });
            } else {
                binding.profileFragmentFriendsOverloaded.setVisibility(View.VISIBLE);
                binding.profileFragmentFriendsList.setVisibility(View.GONE);
                binding.profileFragmentFriendsEmpty.setVisibility(View.GONE);
                binding.profileFragmentFriendsLoading.setVisibility(View.GONE);
            }
        });
        //endregion

        // TODO: Load custom decks
        // TODO: Load achievements

        return binding.getRoot();
    }

    @Override
    public void onPyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        CommonUtils.setText(binding.profileFragmentInputs.usernameInput, pyx.user().nickname);
        CommonUtils.setText(binding.profileFragmentInputs.idCodeInput, Prefs.getString(PK.LAST_ID_CODE, null));
    }

    @Override
    public void onPyxInvalid() {
        this.pyx = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        OverloadedApi.get().removeEventListener(this);
        this.pyx = null;
    }

    @Override
    public boolean goBack() {
        return false;
    }

    @NotNull
    public String getUsername() {
        return CommonUtils.getText(binding.profileFragmentInputs.usernameInput);
    }

    @Nullable
    public String getIdCode() {
        String idCode = CommonUtils.getText(binding.profileFragmentInputs.idCodeInput);
        return idCode.trim().isEmpty() ? null : idCode.trim();
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        switch (event.type) {
            case USER_JOINED_SERVER:
                if (event.data != null && friendsAdapter != null)
                    friendsAdapter.userJoined(event.data.getString("nick"), event.data.getString("server"));
                break;
            case USER_LEFT_SERVER:
                if (event.data != null && friendsAdapter != null)
                    friendsAdapter.userLeft(event.data.getString("nick"));
                break;
            case ADDED_FRIEND:
            case REMOVED_AS_FRIEND:
            case REMOVED_FRIEND:
            case ADDED_AS_FRIEND:
                String username = (String) event.obj;
                if (username == null && event.data != null)
                    username = event.data.getString("username");

                if (username != null && friendsAdapter != null) friendsAdapter.update(username);
                break;
        }
    }

    private class FriendsAdapter extends OrderedRecyclerViewAdapter<FriendsAdapter.ViewHolder, FriendStatus, Void, Void> {
        FriendsAdapter(@NonNull Collection<FriendStatus> list) {
            super(new ArrayList<>(list), null);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        protected boolean matchQuery(@NonNull FriendStatus item, @Nullable String query) {
            return true;
        }

        @Override
        protected void onSetupViewHolder(@NonNull ViewHolder holder, int position, @NonNull FriendStatus friend) {
            holder.binding.friendItemName.setText(friend.username);
            holder.setStatus(friend.getStatus());

            // TODO: Show popup
        }

        @Override
        protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull FriendStatus friend) {
            holder.setStatus(friend.getStatus());
        }

        @Override
        protected void shouldUpdateItemCount(int count) {
            if (count == 0) {
                binding.profileFragmentFriendsList.setVisibility(View.GONE);
                binding.profileFragmentFriendsEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.profileFragmentFriendsList.setVisibility(View.VISIBLE);
                binding.profileFragmentFriendsEmpty.setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public Comparator<FriendStatus> getComparatorFor(@NonNull Void sorting) {
            return (o1, o2) -> {
                int res = o1.getStatus().ordinal() - o2.getStatus().ordinal();
                return res != 0 ? res : o1.username.compareToIgnoreCase(o2.username);
            };
        }

        void userLeft(@NonNull String nickname) {
            itemChanged(elm -> {
                if (elm.username.equals(nickname)) {
                    elm.updateLoggedServer(null);
                    return true;
                }

                return false;
            });
        }

        void userJoined(@NonNull String nickname, @NonNull String serverId) {
            itemChanged(elm -> {
                if (elm.username.equals(nickname)) {
                    elm.updateLoggedServer(serverId);
                    return true;
                }

                return false;
            });
        }

        void update(@NonNull String friend) {
            Map<String, FriendStatus> map = OverloadedApi.get().friendsStatusCache();
            if (map == null) return;

            FriendStatus status = map.get(friend);
            if (status == null) removeItem(elm -> elm.username.equals(friend));
            else itemChangedOrAdded(status);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemFriendBinding binding;

            ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_friend, parent, false));
                binding = ItemFriendBinding.bind(itemView);
            }

            private void setStatus(@NonNull FriendStatus.Status status) {
                int color;
                switch (status) {
                    case INCOMING_REQUEST:
                        color = R.color.orange;
                        break;
                    case ONLINE:
                        color = R.color.green;
                        break;
                    case OFFLINE:
                        color = R.color.red;
                        break;
                    case OUTGOING_REQUEST:
                        color = R.color.deepPurple;
                        break;
                    default:
                        throw new IllegalArgumentException(status.name());
                }

                CommonUtils.setImageTintColor(binding.friendItemStatus, color);
            }
        }
    }

    private class StarredCardsAdapter extends RecyclerView.Adapter<StarredCardsAdapter.ViewHolder> {
        private final StarredCardsDatabase db;
        private final List<StarredCard> list;

        StarredCardsAdapter(StarredCardsDatabase db, List<StarredCard> list) {
            this.db = db;
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StarredCard card = list.get(position);
            holder.binding.starredCardItemText.setHtml(card.textUnescaped());
            holder.binding.starredCardItemUnstar.setOnClickListener(v -> {
                db.remove(card);

                for (int i = 0; i < list.size(); i++) {
                    if (card.equals(list.get(i))) {
                        list.remove(i);
                        notifyItemRemoved(i);
                        return;
                    }
                }
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemStarredCardBinding binding;

            public ViewHolder(@NonNull ViewGroup parent) {
                super(getLayoutInflater().inflate(R.layout.item_starred_card, parent, false));
                binding = ItemStarredCardBinding.bind(itemView);
            }
        }
    }
}
