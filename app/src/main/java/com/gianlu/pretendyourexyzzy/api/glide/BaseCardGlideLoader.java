package com.gianlu.pretendyourexyzzy.api.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;

import java.io.InputStream;
import java.util.regex.Pattern;

public final class BaseCardGlideLoader extends BaseGlideUrlLoader<BaseCard> {
    private static final Pattern IMGUR_PATTERN = Pattern.compile("^http(?:s|)://imgur\\.com/(.*)\\.(.+)$");
    private static final Pattern IS_IMGUR_PATTERN = Pattern.compile("^http(?:s|)://imgur\\.com");

    private BaseCardGlideLoader() {
        super(new HttpGlideUrlLoader());
    }

    @NonNull
    public static String extractUrl(@NonNull String url) {
        if (url.endsWith(".gifv")) {
            return url.substring(0, url.length() - 1);
        } else if (IS_IMGUR_PATTERN.matcher(url).find()) {
            if (!IMGUR_PATTERN.matcher(url).find()) {
                return url + ".png";
            }
        }

        return url;
    }

    @NonNull
    @Override
    protected String getUrl(@NonNull BaseCard card, int width, int height, Options options) {
        String url = card.getImageUrl();
        if (url == null) throw new NullPointerException();
        return extractUrl(url);
    }

    @Override
    public boolean handles(@NonNull BaseCard card) {
        return card.getImageUrl() != null;
    }

    public static class Factory implements ModelLoaderFactory<BaseCard, InputStream> {

        @NonNull
        @Override
        public ModelLoader<BaseCard, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new BaseCardGlideLoader();
        }

        @Override
        public void teardown() {
        }
    }
}
