package com.gianlu.pretendyourexyzzy.overloaded.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.ImagesListView;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.overloaded.AchievementImageLoader;
import com.gianlu.pretendyourexyzzy.overloaded.ChatBottomSheet;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUserProfileBottomSheet;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.games.achievement.Achievement;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.SuccessCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.model.UserData.PropertyKey;

public class ProfileFragment extends FragmentWithDialog implements OverloadedApi.EventListener {
    private static final String TAG = ProfileFragment.class.getSimpleName();
    private ImagesListView achievements;
    private List<String> lastAchievements;
    private ImagesListView linkedAccounts;
    private RecyclerView friendsList;
    private MessageView friendsMessage;
    private ProgressBar friendsLoading;
    private FriendsAdapter friendsAdapter;

    @NonNull
    public static ProfileFragment get(@NonNull Context context) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.profile));
        fragment.setArguments(args);
        return fragment;
    }

    private static void setupPreferencesCheckBox(@NonNull CheckBox checkBox, @NonNull PropertyKey key) {
        UserData data = OverloadedApi.get().userDataCached();
        if (data == null) return;

        checkBox.setChecked(data.getPropertyBoolean(key));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isEnabled()) return;

            buttonView.setEnabled(false);
            OverloadedApi.get().setUserProperty(key, String.valueOf(isChecked), null, new SuccessCallback() {
                @Override
                public void onSuccessful() {
                    buttonView.setChecked(isChecked);
                    buttonView.setEnabled(true);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Log.e(TAG, "Failed updating user property: " + key, ex);

                    buttonView.setChecked(!isChecked); // Revert operation
                    buttonView.setEnabled(true);
                }
            });
        });
    }

    private boolean achievementsChanged(@NonNull List<Achievement> newAchievements) {
        if (lastAchievements == null || newAchievements.size() != lastAchievements.size())
            return true;

        for (Achievement a : newAchievements)
            if (lastAchievements.indexOf(a.getAchievementId()) == -1)
                return true;

        for (String a : lastAchievements)
            if (OverloadedUtils.findAchievement(newAchievements, a) == null)
                return true;

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        GPGamesHelper.loadAchievements(requireContext())
                .addOnCompleteListener(task -> {
                    Iterable<Achievement> result;
                    if (task.isSuccessful()) {
                        result = task.getResult();
                    } else {
                        Log.e(TAG, "Failed getting achievements.", task.getException());
                        result = Collections.emptyList();
                    }

                    List<Achievement> list = OverloadedUtils.getUnlockedAchievements(result);
                    if (achievementsChanged(list)) {
                        lastAchievements = OverloadedUtils.toAchievementsIds(list);

                        AchievementImageLoader il = new AchievementImageLoader(requireContext());
                        CommonUtils.showViewAndLabel(achievements);
                        achievements.removeAllViews();
                        for (Achievement ach : list)
                            achievements.addItem(ach, il);
                    }
                });

        CommonUtils.showViewAndLabel(linkedAccounts);
        linkedAccounts.removeAllViews();
        for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS)
            if (OverloadedApi.get().hasLinkedProvider(provider.id))
                linkedAccounts.addItem(provider.iconRes, ImageView::setImageResource);

        OverloadedApi.get().friendsStatus()
                .addOnSuccessListener(result -> {
                    friendsLoading.setVisibility(View.GONE);
                    friendsList.setAdapter(friendsAdapter = new FriendsAdapter(requireContext(), result.values()));
                })
                .addOnFailureListener(ex -> {
                    friendsAdapter = null;
                    Log.e(TAG, "Failed getting friends status.", ex);

                    friendsLoading.setVisibility(View.GONE);
                    friendsList.setVisibility(View.GONE);
                    friendsMessage.setVisibility(View.VISIBLE);
                    friendsMessage.error(R.string.failedLoading);
                });
    }

    private void updatedItemCount(int count) {
        if (count == 0) {
            friendsList.setVisibility(View.GONE);
            friendsMessage.setVisibility(View.VISIBLE);
            friendsMessage.info(R.string.noFriends_long);
        } else {
            friendsList.setVisibility(View.VISIBLE);
            friendsMessage.setVisibility(View.GONE);
        }
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
        ScrollView layout = (ScrollView) inflater.inflate(R.layout.fragment_overloaded_profile, container, false);
        achievements = layout.findViewById(R.id.overloadedProfileFragment_achievements);
        CommonUtils.hideViewAndLabel(achievements);

        linkedAccounts = layout.findViewById(R.id.overloadedProfileFragment_linkedAccounts);
        CommonUtils.hideViewAndLabel(linkedAccounts);

        friendsMessage = layout.findViewById(R.id.overloadedProfileFragment_friendsMessage);
        friendsLoading = layout.findViewById(R.id.overloadedProfileFragment_friendsLoading);
        friendsList = layout.findViewById(R.id.overloadedProfileFragment_friendsList);
        friendsList.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));

        CheckBox publicStarredCards = layout.findViewById(R.id.overloadedProfileFragment_prefs_publicStarredCards);
        setupPreferencesCheckBox(publicStarredCards, PropertyKey.PUBLIC_STARRED_CARDS);

        CheckBox publicCustomDecks = layout.findViewById(R.id.overloadedProfileFragment_prefs_publicCustomDecks);
        setupPreferencesCheckBox(publicCustomDecks, PropertyKey.PUBLIC_CUSTOM_DECKS);

        return layout;
    }

    @Override
    public void onEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (friendsAdapter == null) return;

        switch (event.type) {
            case USER_JOINED_SERVER:
                if (event.data != null)
                    friendsAdapter.userJoined(event.data.getString("nick"), event.data.getString("server"));
                break;
            case USER_LEFT_SERVER:
                if (event.data != null)
                    friendsAdapter.userLeft(event.data.getString("nick"));
                break;
            case ADDED_FRIEND:
            case REMOVED_AS_FRIEND:
            case REMOVED_FRIEND:
            case ADDED_AS_FRIEND:
                String username = (String) event.obj;
                if (username == null && event.data != null)
                    username = event.data.getString("username");

                if (username != null) friendsAdapter.update(username);
                break;
        }
    }

    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final List<FriendStatus> friends;

        FriendsAdapter(@NonNull Context context, Collection<FriendStatus> friends) {
            this.inflater = LayoutInflater.from(context);
            this.friends = new ArrayList<>(friends);

            updatedItemCount(friends.size());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        void userLeft(@NonNull String nickname) {
            for (int i = 0; i < friends.size(); i++) {
                FriendStatus friend = friends.get(i);
                if (Objects.equals(friend.username, nickname)) {
                    friend.updateLoggedServer(null);
                    notifyItemChanged(i);
                    break;
                }
            }
        }

        void userJoined(@NonNull String nickname, @NonNull String serverId) {
            for (int i = 0; i < friends.size(); i++) {
                FriendStatus friend = friends.get(i);
                if (Objects.equals(friend.username, nickname)) {
                    friend.updateLoggedServer(serverId);
                    notifyItemChanged(i);
                    break;
                }
            }
        }

        void update(@NonNull String username) {
            Map<String, FriendStatus> map = OverloadedApi.get().friendsStatusCache();
            if (map == null) return;

            FriendStatus status = map.get(username);
            if (status == null) {
                for (int i = 0; i < friends.size(); i++) {
                    if (Objects.equals(friends.get(i).username, username)) {
                        friends.remove(i);
                        notifyItemRemoved(i);
                        break;
                    }
                }

                updatedItemCount(friends.size());
            } else if (!friends.contains(status)) {
                friends.add(status);
                notifyItemInserted(friends.size() - 1);
                updatedItemCount(friends.size());
            } else {
                for (int i = 0; i < friends.size(); i++) {
                    if (Objects.equals(friends.get(i).username, username)) {
                        friends.set(i, status);
                        notifyItemChanged(i);
                        break;
                    }
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FriendStatus friend = friends.get(position);
            holder.name.setText(friend.username);

            if (friend.request) {
                CommonUtils.setTextColorFromAttr(holder.status, android.R.attr.textColorSecondary);
                CommonUtils.setText(holder.status, R.string.friendRequest);
            } else {
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
            }

            holder.itemView.setOnClickListener(v -> showPopup(holder.itemView.getContext(), holder.itemView, friend));
        }

        private void showPopup(@NonNull Context context, @NonNull View anchor, @NonNull FriendStatus friend) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.inflate(R.menu.item_overloaded_user);

            Menu menu = popup.getMenu();
            if (!friend.mutual) menu.removeItem(R.id.overloadedUserItemMenu_openChat);
            if (!friend.request) {
                menu.removeItem(R.id.overloadedUserItemMenu_rejectRequest);
                menu.removeItem(R.id.overloadedUserItemMenu_acceptRequest);
            } else {
                menu.removeItem(R.id.overloadedUserItemMenu_removeFriend);
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.overloadedUserItemMenu_showProfile:
                        OverloadedUserProfileBottomSheet.get().show(ProfileFragment.this, friend.username);
                        return true;
                    case R.id.overloadedUserItemMenu_openChat:
                        OverloadedApi.chat(context).startChat(friend.username)
                                .addOnSuccessListener(chat -> {
                                    ChatBottomSheet sheet = new ChatBottomSheet();
                                    sheet.show(getActivity(), chat);
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed opening chat.", ex);
                                    showToast(Toaster.build().message(R.string.failedCreatingChat).extra(friend));
                                });
                        return true;
                    case R.id.overloadedUserItemMenu_rejectRequest:
                    case R.id.overloadedUserItemMenu_removeFriend:
                        OverloadedApi.get().removeFriend(friend.username)
                                .addOnSuccessListener(map -> {
                                    AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_REMOVE_FRIEND);
                                    showToast(Toaster.build().message(R.string.removedFriend).extra(friend));
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed removing friend.", ex);
                                    showToast(Toaster.build().message(R.string.failedRemovingFriend).extra(friend));
                                });
                        return true;
                    case R.id.overloadedUserItemMenu_acceptRequest:
                        OverloadedApi.get().addFriend(friend.username)
                                .addOnSuccessListener(map -> {
                                    AnalyticsApplication.sendAnalytics(OverloadedUtils.ACTION_ADD_FRIEND);
                                    showToast(Toaster.build().message(R.string.friendAdded).extra(friend));
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed adding friend.", ex);
                                    showToast(Toaster.build().message(R.string.failedAddingFriend).extra(friend));
                                });
                        return true;
                    default:
                        return false;
                }
            });

            CommonUtils.showPopupOffset(popup, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics()), 0);
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