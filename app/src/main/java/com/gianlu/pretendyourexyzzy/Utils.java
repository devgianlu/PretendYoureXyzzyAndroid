package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.graphics.Paint;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public final class Utils {
    public static final String ACTION_STARRED_CARD_ADD = "added_starred_card";
    public static final String ACTION_STARRED_CARD_REMOVE = "removed_starred_card";
    public static final String ACTION_JOIN_GAME = "joined_game";
    public static final String ACTION_SPECTATE_GAME = "spectate_game";
    public static final String ACTION_LEFT_GAME = "left_game";
    public static final String ACTION_SENT_GAME_MSG = "sent_message_game";
    public static final String ACTION_JUDGE_CARD = "judged_card";
    public static final String ACTION_PLAY_CUSTOM_CARD = "played_custom_card";
    public static final String ACTION_PLAY_CARD = "played_card";
    public static final String ACTION_SHOW_ROUND = "show_round";
    public static final String ACTION_SAVE_SHARE_ROUND = "save_share_round";
    public static final String ACTION_SENT_MSG = "sent_message";
    public static final String ACTION_UNKNOWN_EVENT = "unknown_server_event";
    public static final String ACTION_BLOCK_USER = "block_user";
    public static final String ACTION_UNBLOCK_USER = "unblock_user";
    public static final String ACTION_IMPORTED_CUSTOM_DECK = "imported_custom_deck";
    public static final String ACTION_EXPORTED_CUSTOM_DECK = "exported_custom_deck";
    public static final String ACTION_CREATED_CUSTOM_DECK = "created_custom_deck";
    public static final String ACTION_DELETED_CUSTOM_DECK = "created_custom_deck";
    public static final String ACTION_ADDED_CUSTOM_DECK_TEXT_CARD = "added_custom_deck_text_card";
    public static final String ACTION_ADDED_CUSTOM_DECK_IMAGE_CARD = "added_custom_deck_image_card";
    public static final String ACTION_REMOVED_CUSTOM_DECK_CARD = "added_custom_deck_card";
    public static final String ACTION_CR_CAST_LOGIN = "cr_cast_login";
    public static final String ACTION_CR_CAST_LOGOUT = "cr_cast_logout";
    public static final String ACTION_ADDED_FRIEND_USERNAME = "added_friend_username";

    private Utils() {
    }

    @NonNull
    public static String sha1(@NonNull String str) {
        try {
            return Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(str.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Nullable
    public static String myPyxUsername() {
        try {
            return RegisteredPyx.get().user().nickname;
        } catch (LevelMismatchException ex) {
            return null;
        }
    }

    @NonNull
    public static String getDisplayableName(@NonNull FirebaseUser user) {
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = user.getEmail();
            if (name == null || name.isEmpty())
                name = user.getUid();
        }

        return name;
    }

    @NonNull
    public static String buildDeckCountString(int decks, int black, int white) {
        StringBuilder builder = new StringBuilder();
        builder.append(decks).append(" deck");
        if (decks != 1) builder.append("s");
        builder.append(", ");

        builder.append(black).append(" black card");
        if (black != 1) builder.append("s");
        builder.append(", ");

        builder.append(white).append(" white card");
        if (white != 1) builder.append("s");

        return builder.toString();
    }

    public static void generateUsernamePlaceholders(@NotNull Context context, @NotNull ViewGroup container, int fontSizeSp, int spacingDip, int amount) {
        Paint paint = new Paint();
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_regular));
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSizeSp, context.getResources().getDisplayMetrics()));
        Paint.FontMetrics fm = paint.getFontMetrics();

        int height = (int) Math.ceil(fm.bottom - fm.top + fm.leading);
        int spacingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, spacingDip, context.getResources().getDisplayMetrics());
        int dp12 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, spacingDip, context.getResources().getDisplayMetrics());

        for (int i = 0; i < amount; i++) {
            int width = dp12 * ThreadLocalRandom.current().nextInt(8, 20);

            View view = new View(context);
            view.setBackgroundResource(R.drawable.placeholder_general_square_item);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(0, 0, 0, spacingPx);
            container.addView(view, params);
        }
    }
}
