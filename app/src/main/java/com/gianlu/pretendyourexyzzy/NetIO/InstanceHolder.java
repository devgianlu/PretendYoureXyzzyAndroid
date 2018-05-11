package com.gianlu.pretendyourexyzzy.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;

class InstanceHolder {
    private static InstanceHolder holder;
    private Pyx pyx = null;

    private InstanceHolder() {
    }

    @NonNull
    public static InstanceHolder holder() {
        if (holder == null) holder = new InstanceHolder();
        return holder;
    }

    @NonNull
    public synchronized Pyx instantiateStandard(Context context) {
        if (pyx == null) pyx = new Pyx(context);
        return pyx;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public synchronized <P extends Pyx> P get(@NonNull Level level) throws LevelMismatchException {
        if (atLeast(level)) return (P) pyx;
        else throw new LevelMismatchException(level(), level);
    }

    public synchronized boolean atLeast(@NonNull Level level) {
        return level().importance >= level.importance;
    }

    @NonNull
    public synchronized Level level() {
        if (pyx == null) return Level.MISSING;
        else if (pyx instanceof RegisteredPyx) return Level.REGISTERED;
        else if (pyx instanceof FirstLoadedPyx) return Level.FIRST_LOADED;
        else return Level.STANDARD;
    }

    public synchronized void set(Pyx pyx) {
        this.pyx = pyx;
    }

    public synchronized void invalidate() {
        if (pyx != null) pyx.close();
        pyx = null;
    }

    public enum Level {
        MISSING(0),
        STANDARD(1),
        FIRST_LOADED(2),
        REGISTERED(3);

        final int importance;

        Level(int importance) {
            this.importance = importance;
        }
    }
}
