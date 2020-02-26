package com.gianlu.pretendyourexyzzy.overloaded.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.ImagesListView;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.overloaded.AchievementImageLoader;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.api.FriendsStatusCallback;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils;
import com.google.android.gms.games.achievement.Achievement;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfileFragment extends FragmentWithDialog implements OverloadedApi.EventListener {
    private ImagesListView achievements;
    private ImagesListView linkedAccounts;
    private RecyclerView friends;
    private FriendsAdapter friendsAdapter;

    @NonNull
    public static ProfileFragment get(@NonNull Context context) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.profile));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        GPGamesHelper.loadAchievements(requireContext(), getActivity(), new GPGamesHelper.LoadIterable<Achievement>() {
            @Override
            public void onLoaded(@NonNull Iterable<Achievement> result) {
                AchievementImageLoader il = new AchievementImageLoader(requireContext());

                CommonUtils.showViewAndLabel(achievements);
                achievements.removeAllViews();
                for (Achievement ach : OverloadedUtils.getUnlockedAchievements(result))
                    achievements.addItem(ach, il);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                Logging.log(ex);
                onLoaded(Collections.emptyList());
            }
        });

        CommonUtils.showViewAndLabel(linkedAccounts);
        linkedAccounts.removeAllViews();
        for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS)
            if (OverloadedApi.get().hasLinkedProvider(provider.id))
                linkedAccounts.addItem(provider.iconRes, ImageView::setImageResource);

        OverloadedApi.get().friendsStatus(getActivity(), new FriendsStatusCallback() {
            @Override
            public void onFriendsStatus(@NotNull Map<String, OverloadedApi.FriendStatus> result) {
                if (result.isEmpty()) {
                    CommonUtils.hideViewAndLabel(friends);
                } else {
                    CommonUtils.showViewAndLabel(friends);
                    friends.setAdapter(friendsAdapter = new FriendsAdapter(requireContext(), result.values()));
                }
            }

            @Override
            public void onFailed(@NotNull Exception ex) {
                friendsAdapter = null;
                Logging.log(ex);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        OverloadedApi.get().addEventListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OverloadedApi.get().removeEventListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_overloaded_profile, container, false);
        achievements = layout.findViewById(R.id.overloadedProfileFragment_achievements);
        CommonUtils.hideViewAndLabel(achievements);

        linkedAccounts = layout.findViewById(R.id.overloadedProfileFragment_linkedAccounts);
        CommonUtils.hideViewAndLabel(linkedAccounts);

        friends = layout.findViewById(R.id.overloadedProfileFragment_friends);
        friends.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        CommonUtils.hideViewAndLabel(friends);

        return layout;
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (friendsAdapter == null) return;

        if (event.type == OverloadedApi.Event.Type.USER_JOINED_SERVER) {
            friendsAdapter.userJoined(event.obj.getString("nick"), event.obj.getString("server"));
        } else if (event.type == OverloadedApi.Event.Type.USER_LEFT_SERVER) {
            friendsAdapter.userLeft(event.obj.getString("nick"));
        }
    }

    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final List<OverloadedApi.FriendStatus> friends;

        FriendsAdapter(@NonNull Context context, Collection<OverloadedApi.FriendStatus> friends) {
            this.inflater = LayoutInflater.from(context);
            this.friends = new ArrayList<>(friends);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        void userLeft(@NonNull String nickname) {
            for (int i = 0; i < friends.size(); i++) {
                OverloadedApi.FriendStatus friend = friends.get(i);
                if (Objects.equals(friend.username, nickname)) {
                    friend.update(null);
                    notifyItemChanged(i);
                }
            }
        }

        void userJoined(@NonNull String nickname, @NonNull String serverId) {
            for (int i = 0; i < friends.size(); i++) {
                OverloadedApi.FriendStatus friend = friends.get(i);
                if (Objects.equals(friend.username, nickname)) {
                    friend.update(serverId);
                    notifyItemChanged(i);
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OverloadedApi.FriendStatus friend = friends.get(position);
            holder.name.setText(friend.username);

            if (friend.mutual) {
                if (friend.serverId != null) {
                    CommonUtils.setTextColor(holder.status, R.color.green);
                    Pyx.Server server = Pyx.Server.fromOverloadedId(friend.serverId);
                    CommonUtils.setText(holder.status, R.string.friendOnlineOn, server == null ? friend.serverId : server.name);
                } else {
                    CommonUtils.setTextColor(holder.status, R.color.red);
                    CommonUtils.setText(holder.status, R.string.friendOffline);
                }
            } else {
                CommonUtils.setTextColorFromAttr(holder.status, android.R.attr.textColorSecondary);
                CommonUtils.setText(holder.status, R.string.notMutual);
            }

            holder.itemView.setOnClickListener(v -> showPopup(holder.itemView.getContext(), holder.itemView, friend.username));
        }

        private void showPopup(@NonNull Context context, @NonNull View anchor, @NonNull String username) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.inflate(R.menu.item_overloaded_user);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.overloadedUserItemMenu_showProfile:
                        // TODO: Show user profile
                        return true;
                    case R.id.overloadedUserItemMenu_openChat:
                        // TODO: Open chat with user
                        return true;
                    case R.id.overloadedUserItemMenu_removeFriend:
                        OverloadedApi.get().removeFriend(username, null, new FriendsStatusCallback() {
                            @Override
                            public void onFriendsStatus(@NotNull Map<String, OverloadedApi.FriendStatus> result) {
                                showToast(Toaster.build().message(R.string.removedFriend).extra(username));
                                if (friendsAdapter != null) friendsAdapter.removeUser(username);
                            }

                            @Override
                            public void onFailed(@NotNull Exception ex) {
                                showToast(Toaster.build().message(R.string.failedRemovingFriend).ex(ex).extra(username));
                            }
                        });
                        return true;
                    default:
                        return false;
                }
            });

            CommonUtils.showPopupOffset(popup, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics()), 0);
        }

        private void removeUser(@NonNull String username) {
            for (int i = 0; i < friends.size(); i++) {
                if (Objects.equals(friends.get(i).username, username)) {
                    friends.remove(i);
                    notifyItemRemoved(i);
                    return;
                }
            }
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView status;

            ViewHolder(@NonNull ViewGroup parent) {
                super(inflater.inflate(R.layout.item_overloaded_friend, parent, false));

                name = itemView.findViewById(R.id.overloadedFriendItem_name);
                status = itemView.findViewById(R.id.overloadedFriendItem_status);
            }
        }
    }
}