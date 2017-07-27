package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class Prefs {
    private static SharedPreferences prefs;

    public static boolean getBoolean(Context context, Keys key, boolean fallback) {
        init(context);
        return prefs.getBoolean(key.key, fallback);
    }

    public static String getString(Context context, Keys key, String fallback) {
        init(context);
        return prefs.getString(key.key, fallback);
    }

    public static void putString(Context context, Keys key, String value) {
        init(context);
        prefs.edit().putString(key.key, value).apply();
    }

    private static void init(Context context) {
        if (prefs != null) return;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getInt(Context context, Keys key, int fallback) {
        init(context);
        return prefs.getInt(key.key, fallback);
    }

    public static int getFakeInt(Context context, Keys key, int fallback) {
        init(context);
        return Integer.parseInt(prefs.getString(key.key, String.valueOf(fallback)));
    }

    @SuppressWarnings("ConstantConditions")
    public static void removeFromSet(Context context, Keys key, String value) {
        init(context);
        Set<String> set = new HashSet<>(getSet(context, key, new HashSet<String>()));
        set.remove(value);
        prefs.edit().putStringSet(key.key, set).apply();
    }

    @SuppressWarnings("ConstantConditions")
    public static void addToSet(Context context, Keys key, String value) {
        init(context);
        Set<String> set = new HashSet<>(getSet(context, key, new HashSet<String>()));
        if (!set.contains(value)) set.add(value);
        prefs.edit().putStringSet(key.key, set).apply();
    }

    public static Set<String> getSet(Context context, Keys key, @Nullable Set<String> fallback) {
        init(context);
        Set<String> set = prefs.getStringSet(key.key, fallback);
        if (set == null) return null;
        return new HashSet<>(set);
    }

    public static void putSet(Context context, Keys key, Set<String> set) {
        init(context);
        prefs.edit().putStringSet(key.key, set).apply();
    }

    public static void remove(Context context, Keys key) {
        init(context);
        prefs.edit().remove(key.key).apply();
    }

    public enum Keys {
        LAST_NICKNAME("lastNickname"),
        LAST_SERVER("lastServer");
        public final String key;

        Keys(String key) {
            this.key = key;
        }
    }
}
