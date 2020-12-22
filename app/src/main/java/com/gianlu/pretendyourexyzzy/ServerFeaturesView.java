package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.ServersChecker;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.EnumSet;

public final class ServerFeaturesView extends View {
    private final EnumSet<Feature> features = EnumSet.noneOf(Feature.class);
    private final EnumMap<Feature, Drawable> drawables = new EnumMap<>(Feature.class);
    private final int mActiveColor;
    private final int mInactiveColor;

    public ServerFeaturesView(Context context) {
        this(context, null, 0);
    }

    public ServerFeaturesView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ServerFeaturesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ServerFeaturesView, defStyleAttr, 0);
        try {
            mActiveColor = a.getColor(R.styleable.ServerFeaturesView_activeColor, Color.BLACK);
            mInactiveColor = a.getColor(R.styleable.ServerFeaturesView_inactiveColor, Color.rgb(240, 240, 240));
        } finally {
            a.recycle();
        }

        for (Feature feature : Feature.values())
            drawables.put(feature, ContextCompat.getDrawable(context, feature.res));
    }

    public void setFeatures(@NotNull Pyx.Server server) {
        features.clear();
        if (server.hasMetrics())
            features.add(Feature.METRICS);

        if (server.status != null && server.status.stats != null) {
            ServersChecker.CheckResult.Stats stats = server.status.stats;
            if (stats.gameChatEnabled()) features.add(Feature.GAME_CHAT);
            if (stats.globalChatEnabled()) features.add(Feature.GLOBAL_CHAT);
            if (stats.blankCardsEnabled()) features.add(Feature.BLANK_CARDS);
            if (stats.customDecksEnabled() || stats.crCastEnabled())
                features.add(Feature.CUSTOM_DECKS);
        }

        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // canvas.drawColor(Color.WHITE);
        int size = Math.min(getHeight() / 3, getWidth() / 3);

        for (Feature feature : Feature.values()) {
            Drawable drawable = drawables.get(feature);
            if (drawable == null) continue;

            drawable.setColorFilter(features.contains(feature) ? mActiveColor : mInactiveColor, PorterDuff.Mode.SRC_IN);

            int left = size * feature.x;
            int top = size * feature.y;
            drawable.setBounds(left, top, left + size, top + size);
            drawable.draw(canvas);
        }
    }

    private enum Feature {
        METRICS(R.drawable.baseline_person_24, 1, 1),
        GAME_CHAT(R.drawable.baseline_chat_bubble_outline_24, 0, 0),
        GLOBAL_CHAT(R.drawable.baseline_chat_24, 2, 0),
        BLANK_CARDS(R.drawable.baseline_edit_24, 0, 2),
        CUSTOM_DECKS(R.drawable.baseline_bookmarks_24, 2, 2);

        final int res, x, y;

        Feature(@DrawableRes int res, int x, int y) {
            this.res = res;
            this.x = x;
            this.y = y;
        }
    }
}
