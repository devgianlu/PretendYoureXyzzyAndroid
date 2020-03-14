package com.gianlu.pretendyourexyzzy.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BlockedUsers;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.models.Name;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.FriendsStatusCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public class NamesAdapter extends OrderedRecyclerViewAdapter<NamesAdapter.ViewHolder, Name, NamesAdapter.Sorting, String> {
    private final LayoutInflater inflater;
    private final List<String> overloadedUsers;
    private final Listener listener;

    public NamesAdapter(Context context, List<Name> names, @NonNull List<String> overloadedUsers, Listener listener) {
        super(names, Sorting.AZ);
        this.inflater = LayoutInflater.from(context);
        this.overloadedUsers = overloadedUsers;
        this.listener = listener;
    }

    @Override
    @NonNull
    public NamesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
            CommonUtils.setTextColor((TextView) holder.itemView, R.color.appColorBright);
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
                menu.removeItem(R.id.nameItemMenu_addFriend);
            } else {
                menu.removeItem(R.id.nameItemMenu_unblock);
                if (overloadedUsers.contains(username)) {
                    Map<String, FriendStatus> map = OverloadedApi.get().friendsStatusCache();
                    if (map != null && map.containsKey(username))
                        menu.removeItem(R.id.nameItemMenu_addFriend);
                } else {
                    menu.removeItem(R.id.nameItemMenu_addFriend);
                }
            }
        } else {
            menu.removeItem(R.id.nameItemMenu_unblock);
            menu.removeItem(R.id.nameItemMenu_block);
            menu.removeItem(R.id.nameItemMenu_addFriend);
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.nameItemMenu_showInfo:
                    listener.onShowUserInfo(username);
                    return true;
                case R.id.nameItemMenu_unblock:
                    BlockedUsers.unblock(username);
                    return true;
                case R.id.nameItemMenu_block:
                    BlockedUsers.block(username);
                    return true;
                case R.id.nameItemMenu_addFriend:
                    OverloadedApi.get().addFriend(username, null, new FriendsStatusCallback() {
                        @Override
                        public void onFriendsStatus(@NotNull Map<String, FriendStatus> result) {
                            listener.showToast(Toaster.build().message(R.string.friendAdded).extra(username));
                        }

                        @Override
                        public void onFailed(@NotNull Exception ex) {
                            listener.showToast(Toaster.build().message(R.string.failedAddingFriend).ex(ex).extra(username));
                        }
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
        if (listener != null) listener.shouldUpdateItemCount(count);
    }

    @NonNull
    @Override
    public Comparator<Name> getComparatorFor(Sorting sorting) {
        switch (sorting) {
            default:
            case AZ:
                return new Name.AzComparator();
            case ZA:
                return new Name.ZaComparator();
        }
    }

    public void removeItem(String name) {
        Iterator<Name> iter = originalObjs.iterator();
        while (iter.hasNext()) {
            if (name.equals(iter.next().noSigil())) {
                iter.remove();
                break;
            }
        }
    }

    public void overloadedUserLeft(@NonNull String nick) {
        if (!overloadedUsers.remove(nick)) return;

        for (Name name : originalObjs) {
            if (name.noSigil().equals(nick)) {
                itemChangedOrAdded(name);
                break;
            }
        }
    }

    public void overloadedUserJoined(@NonNull String nick) {
        if (!overloadedUsers.add(nick)) return;

        for (Name name : originalObjs) {
            if (name.noSigil().equals(nick)) {
                itemChangedOrAdded(name);
                break;
            }
        }
    }

    public void setOverloadedUsers(@NonNull List<String> list) {
        overloadedUsers.clear();
        overloadedUsers.addAll(list);
        notifyDataSetChanged();
    }

    public enum Sorting {
        AZ,
        ZA
    }

    public interface Listener extends DialogUtils.ShowStuffInterface {
        void shouldUpdateItemCount(int count);

        void onShowUserInfo(@NonNull String nickname);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_name, parent, false));
        }
    }
}
