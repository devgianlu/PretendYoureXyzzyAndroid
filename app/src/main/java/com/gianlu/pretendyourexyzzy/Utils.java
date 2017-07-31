package com.gianlu.pretendyourexyzzy;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;

import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.cookie.Cookie;

public class Utils {

    @Nullable
    public static Cookie findCookie(CookieStore store, String name) {
        for (Cookie cookie : store.getCookies())
            if (Objects.equals(cookie.getName(), name))
                return cookie;

        return null;
    }

    @Nullable
    public static GameInfo.Player find(List<GameInfo.Player> players, String name) {
        for (GameInfo.Player player : players)
            if (Objects.equals(player.name, name))
                return player;

        return null;
    }

    @Nullable
    public static Game findGame(List<Game> sets, int id) {
        for (Game game : sets)
            if (Objects.equals(game.gid, id))
                return game;

        return null;
    }

    @Nullable
    public static CardSet find(List<CardSet> sets, int id) {
        for (CardSet set : sets)
            if (Objects.equals(set.id, id))
                return set;

        return null;
    }

    public static class Messages {
        public static final Toaster.Message FAILED_LOADING = new Toaster.Message(R.string.failedLoading, true);
        public static final Toaster.Message FAILED_SEND_MESSAGE = new Toaster.Message(R.string.failedSendMessage, true);
        public static final Toaster.Message FAILED_JOINING = new Toaster.Message(R.string.failedJoining, true);
        public static final Toaster.Message WRONG_PASSWORD = new Toaster.Message(R.string.wrongPassword, false);
        public static final Toaster.Message GAME_FULL = new Toaster.Message(R.string.gameFull, false);
        public static final Toaster.Message FAILED_SPECTATING = new Toaster.Message(R.string.failedSpectating, true);
        public static final Toaster.Message FAILED_LEAVING = new Toaster.Message(R.string.failedLeaving, true);
        public static final Toaster.Message NO_STARRED_CARDS = new Toaster.Message(R.string.noStarredCards, false);
        public static final Toaster.Message HURRY_UP = new Toaster.Message(R.string.hurryUp, false);
        public static final Toaster.Message FAILED_CREATING_GAME = new Toaster.Message(R.string.failedCreatingGame, true);
        public static final Toaster.Message FAILED_START_GAME = new Toaster.Message(R.string.failedStartGame, true);
    }
}
