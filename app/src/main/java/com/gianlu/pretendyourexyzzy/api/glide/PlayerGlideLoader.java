package com.gianlu.pretendyourexyzzy.api.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import java.io.InputStream;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public final class PlayerGlideLoader extends BaseGlideUrlLoader<GameInfo.Player> {
    protected PlayerGlideLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
        super(concreteLoader);
    }

    @Nullable
    @Override
    protected String getUrl(@NonNull GameInfo.Player player, int width, int height, Options options) {
        if (player.name.equals(OverloadedApi.get().username()) || OverloadedApi.get().isOverloadedUserOnServerCached(player.name))
            return OverloadedUtils.getProfileImageUrl(player.name);
        else
            return null;
    }

    @Override
    public boolean handles(@NonNull GameInfo.Player player) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<GameInfo.Player, InputStream> {

        @NonNull
        @Override
        public ModelLoader<GameInfo.Player, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new PlayerGlideLoader(multiFactory.build(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
        }
    }
}
