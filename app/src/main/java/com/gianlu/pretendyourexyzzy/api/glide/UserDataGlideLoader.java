package com.gianlu.pretendyourexyzzy.api.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

import xyz.gianlu.pyxoverloaded.model.UserData;

public final class UserDataGlideLoader extends BaseGlideUrlLoader<UserData> {
    protected UserDataGlideLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
        super(concreteLoader);
    }

    @Nullable
    @Override
    protected String getUrl(@NonNull UserData data, int width, int height, Options options) {
        return data.profileImageId == null ? null : OverloadedUtils.getImageUrl(data.profileImageId);
    }

    @Override
    public boolean handles(@NonNull UserData data) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<UserData, InputStream> {

        @NonNull
        @Override
        public ModelLoader<UserData, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new UserDataGlideLoader(multiFactory.build(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
        }
    }
}
