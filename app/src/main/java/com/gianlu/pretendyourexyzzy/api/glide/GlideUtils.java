package com.gianlu.pretendyourexyzzy.api.glide;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.gianlu.pretendyourexyzzy.R;

import org.jetbrains.annotations.NotNull;

public final class GlideUtils {
    private static final String TAG = GlideUtils.class.getSimpleName();
    private static Drawable profilePlaceholderDrawable = null;

    private GlideUtils() {
    }

    public static void loadProfileImage(@NotNull ImageView into, @NotNull Object model) {
        if (profilePlaceholderDrawable == null) {
            profilePlaceholderDrawable = ContextCompat.getDrawable(into.getContext(), R.drawable.ic_person_circle_900_96);
            if (profilePlaceholderDrawable != null)
                profilePlaceholderDrawable.setTint(Color.rgb(161, 161, 161));
        }

        Glide.with(into)
                .load(model).circleCrop()
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
