package com.gianlu.pretendyourexyzzy.api.models.cards;

import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseCard implements Serializable {
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("^<img.+src='(.*?)'.+/>$");
    private static final Pattern HTML_UNESCAPE_PATTERN = Pattern.compile("&(.+?);");
    private transient String imageUrl = null;
    private transient Spanned textUnescaped = null;

    @NotNull
    private static String unescapeEntities(@NotNull String text) {
        Matcher matcher = HTML_UNESCAPE_PATTERN.matcher(text);
        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                matcher.appendReplacement(sb, Html.fromHtml(matcher.group()).toString());
            } while (matcher.find());
            matcher.appendTail(sb);
            return sb.toString();
        } else {
            return text;
        }
    }

    @NonNull
    public abstract String text();

    @NonNull
    public final Spanned textUnescaped() {
        if (textUnescaped == null) textUnescaped = Html.fromHtml(unescapeEntities(text()));
        return textUnescaped;
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
