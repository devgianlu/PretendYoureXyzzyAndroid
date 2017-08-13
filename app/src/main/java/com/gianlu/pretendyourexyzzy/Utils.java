package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.cardcastapi.Cardcast;
import com.gianlu.cardcastapi.Models.Card;
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

    public static String getCategoryFormal(Context context, Cardcast.Category category) {
        switch (category) {
            case BOOKS:
                return context.getString(R.string.books);
            case COMMUNITY:
                return context.getString(R.string.community);
            case GAMING:
                return context.getString(R.string.gaming);
            case MOVIES:
                return context.getString(R.string.movies);
            case MUSIC:
                return context.getString(R.string.music);
            case SPORTS:
                return context.getString(R.string.sports);
            case TECHNOLOGY:
                return context.getString(R.string.technology);
            case TELEVISION:
                return context.getString(R.string.television);
            case TRANSLATION:
                return context.getString(R.string.translation);
            default:
            case OTHER:
                return context.getString(R.string.other);
            case RANDOM:
                return context.getString(R.string.random);
        }
    }

    @NonNull
    public static String composeCardcastDeckSentence(Card blackCard, Card whiteCard) {
        StringBuilder builder = new StringBuilder();
        builder.append(blackCard.text.get(0));
        builder.append("<u>").append(whiteCard.text.get(0)).append("</u>");
        if (blackCard.text.size() > 1) builder.append(blackCard.text.get(1));

        return builder.toString();
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
        public static final Toaster.Message GAME_STARTED = new Toaster.Message(R.string.gameStarted, false);
        public static final Toaster.Message FAILED_SHARING = new Toaster.Message(R.string.failedSharing, true);
        public static final Toaster.Message OPTIONS_CHANGED = new Toaster.Message(R.string.optionsChanged, false);
        public static final Toaster.Message FAILED_CHANGING_OPTIONS = new Toaster.Message(R.string.failedChangingOptions, true);
        public static final Toaster.Message CARDCAST_ADDED = new Toaster.Message(R.string.cardCastAdded, false);
        public static final Toaster.Message FAILED_ADDING_CARDCAST = new Toaster.Message(R.string.failedAddingCardCast, true);
        public static final Toaster.Message NOT_ENOUGH_PLAYERS = new Toaster.Message(R.string.notEnoughPlayers, false);
    }
}
