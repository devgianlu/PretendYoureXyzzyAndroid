package com.gianlu.pretendyourexyzzy.api.glide;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.jetbrains.annotations.NotNull;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;

public final class GlideUtils {
    private static final String TAG = GlideUtils.class.getSimpleName();
    private static Drawable profilePlaceholderDrawable = null;

    private GlideUtils() {
    }

    public static void loadProfileImage(@NotNull ImageView into, @NotNull GameInfo.Player model) {
        if (model.name.equals(OverloadedApi.get().username()) || OverloadedApi.get().isOverloadedUserOnServerCached(model.name))
            loadProfileImageInternal(into, OverloadedUtils.getProfileImageUrl(model.name));
        else
            loadProfileImageInternal(into, null);
    }

    public static void loadProfileImage(@NotNull ImageView into, @NotNull FriendStatus friend) {
        loadProfileImageInternal(into, OverloadedUtils.getProfileImageUrl(friend.username));
    }

    public static void loadProfileImage(@NotNull ImageView into, @NotNull String username) {
        if (OverloadedApi.get().isOverloadedUserOnServerCached(username) || OverloadedApi.get().hasFriendCached(username))
            loadProfileImageInternal(into, OverloadedUtils.getProfileImageUrl(username));
        else
            loadProfileImageInternal(into, null);
    }

    public static void loadProfileImage(@NotNull ImageView into, @NotNull UserData data) {
        if (data.profileImageId != null)
            loadProfileImageInternal(into, OverloadedUtils.getImageUrl(data.profileImageId));
        else
            loadProfileImageInternal(into, null);
    }

    private static void loadProfileImageInternal(@NotNull ImageView into, @Nullable String url) {
        if (profilePlaceholderDrawable == null) {
            profilePlaceholderDrawable = ContextCompat.getDrawable(into.getContext(), R.drawable.ic_person_circle_900_96);
            if (profilePlaceholderDrawable != null)
                profilePlaceholderDrawable.setTint(Color.rgb(161, 161, 161));
        }

        if (url == null) {
            into.setImageDrawable(profilePlaceholderDrawable);
        } else {
            Glide.with(into)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .circleCrop()
                    .placeholder(profilePlaceholderDrawable)
                    .error(profilePlaceholderDrawable)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException ex, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.d(TAG, "Failed loading profile image, model: " + model, ex);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(into);
        }
    }
}
