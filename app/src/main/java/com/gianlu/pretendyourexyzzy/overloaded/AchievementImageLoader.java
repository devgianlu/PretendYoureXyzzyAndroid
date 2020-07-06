package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.adapters.ImagesListView;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.achievement.Achievement;

import org.jetbrains.annotations.NotNull;

public final class AchievementImageLoader implements ImagesListView.ImageLoader<Achievement> {
    private final ImageManager im;

    public AchievementImageLoader(@NotNull Context context) {
        im = ImageManager.create(context);
    }

    @Override
    public void load(@NonNull ImageView view, @NotNull Achievement obj) {
        im.loadImage(view, obj.getUnlockedImageUri());
    }
}
