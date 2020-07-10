package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.NameValuePair;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class Utils {
    public static final String ACTION_STARRED_CARD_ADD = "added_starred_card";
    public static final String ACTION_STARRED_CARD_REMOVE = "removed_starred_card";
    public static final String ACTION_JOIN_GAME = "joined_game";
    public static final String ACTION_SPECTATE_GAME = "spectate_game";
    public static final String ACTION_LEFT_GAME = "left_game";
    public static final String ACTION_SKIP_TUTORIAL = "skipped_tutorial";
    public static final String ACTION_DONE_TUTORIAL = "did_tutorial";
    public static final String ACTION_SENT_GAME_MSG = "sent_message_game";
    public static final String ACTION_ADDED_CUSTOM_DECK = "added_custom_deck";
    public static final String ACTION_JUDGE_CARD = "judged_card";
    public static final String ACTION_PLAY_CUSTOM_CARD = "played_custom_card";
    public static final String ACTION_PLAY_CARD = "played_card";
    public static final String ACTION_SHOW_ROUND = "show_round";
    public static final String ACTION_SAVE_SHARE_ROUND = "save_share_round";
    public static final String ACTION_SENT_MSG = "sent_message";
    public static final String ACTION_OPEN_URBAN_DICT = "opened_urban_dict_sheet";
    public static final String ACTION_UNKNOWN_EVENT = "unknown_server_event";
    public static final String ACTION_BLOCK_USER = "block_user";
    public static final String ACTION_UNBLOCK_USER = "unblock_user";
    public static final String ACTION_IMPORTED_CUSTOM_DECK = "imported_custom_deck";
    public static final String ACTION_CREATED_CUSTOM_DECK = "created_custom_deck";
    public static final String ACTION_DELETED_CUSTOM_DECK = "created_custom_deck";
    public static final String ACTION_ADDED_CUSTOM_DECK_CARD = "added_custom_deck_card";
    public static final String ACTION_REMOVED_CUSTOM_DECK_CARD = "added_custom_deck_card";
    private static final String TAG = Utils.class.getSimpleName();

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

    public static List<NameValuePair> splitQuery(@NonNull URL url) {
        return splitQuery(url.getQuery());
    }

    public static List<NameValuePair> splitQuery(@NonNull String query) {
        try {
            List<NameValuePair> queryPairs = new ArrayList<>();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0)
                    queryPairs.add(new NameValuePair(
                            URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8")));
            }

            return queryPairs;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @NonNull
    public static String formQuery(List<NameValuePair> pairs) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        try {
            for (NameValuePair pair : pairs) {
                if (!first) builder.append("&");
                builder.append(URLEncoder.encode(pair.key(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(pair.value(""), "UTF-8"));

                first = false;
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        return builder.toString();
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

    public static boolean shouldShowChangelog(@NonNull Context context) {
        try {
            int appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int latestShown = Prefs.getInt(PK.LAST_CHANGELOG_SHOWN, -1);
            return appVersion > latestShown;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Failed getting package info.", ex);
            return false;
        }
    }

    public static void changelogShown(@NonNull Context context) {
        try {
            int appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            Prefs.putInt(PK.LAST_CHANGELOG_SHOWN, appVersion);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Failed getting package info.", ex);
        }
    }

    @Nullable
    public static String getChangelog(@NonNull Context context) {
        try {
            return CommonUtils.readEntirely(context.getResources().openRawResource(R.raw.changelog)).replace("\n", "<br>");
        } catch (IOException ex) {
            Log.e(TAG, "Failed reading changelog.", ex);
            return null;
        }
    }
}
