package com.gianlu.pretendyourexyzzy.api.models.cards;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseCard implements Serializable {
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("^<img.+src='(.*?)'.+/>$");
    private static final String TAG = BaseCard.class.getSimpleName();
    private transient String imageUrl = null;

    @NonNull
    public abstract String text();

    @NonNull
    public final String textUnescaped() {
        try {
            return URLDecoder.decode(text(), "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
            Log.e(TAG, "Failed unescaping text: " + text(), ex);
            return text();
        }
    }

    @Nullable
    public abstract String watermark();

    public abstract int numPick();

    public abstract int numDraw();

    public abstract boolean black();

    @Nullable
    public final String getImageUrl() {
        if (black()) return null;

        if (imageUrl == null) {
            String text = text();
            if (text.startsWith("[img]") && text.endsWith("[/img]")) {
                return imageUrl = text.substring(5, text.length() - 6);
            } else {
                Matcher matcher = HTML_IMAGE_PATTERN.matcher(text);
                if (matcher.find()) {
                    return imageUrl = matcher.group(1);
                }
            }

            return null;
        } else {
            return imageUrl;
        }
    }
}
