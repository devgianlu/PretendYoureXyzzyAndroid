package com.gianlu.pretendyourexyzzy.api.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import java.io.InputStream;

import xyz.gianlu.pyxoverloaded.model.FriendStatus;

public final class FriendStatusGlideLoader extends BaseGlideUrlLoader<FriendStatus> {
    protected FriendStatusGlideLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
        super(concreteLoader);
    }

    @NonNull
    @Override
    protected String getUrl(@NonNull FriendStatus friendStatus, int width, int height, Options options) {
        return OverloadedUtils.getProfileImageUrl(friendStatus.username);
    }

    @Override
    public boolean handles(@NonNull FriendStatus friendStatus) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<FriendStatus, InputStream> {

        @NonNull
        @Override
        public ModelLoader<FriendStatus, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new FriendStatusGlideLoader(multiFactory.build(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
        }
    }
}
