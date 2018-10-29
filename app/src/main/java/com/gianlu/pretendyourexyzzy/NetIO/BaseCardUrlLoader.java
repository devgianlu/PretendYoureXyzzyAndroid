package com.gianlu.pretendyourexyzzy.NetIO;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;

import java.io.InputStream;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

public class BaseCardUrlLoader extends BaseGlideUrlLoader<BaseCard> {
    private static final Pattern IMGUR_PATTERN = Pattern.compile("^http(?:s|)://imgur\\.com/(.*)\\.(.+)$");
    private static final Pattern IS_IMGUR_PATTERN = Pattern.compile("^http(?:s|)://imgur\\.com");

    private BaseCardUrlLoader() {
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

    @Override
    protected String getUrl(BaseCard card, int width, int height, Options options) {
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
            return new BaseCardUrlLoader();
        }

        @Override
        public void teardown() {
        }
    }
}
