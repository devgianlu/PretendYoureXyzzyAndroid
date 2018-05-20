package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTarget;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardSet;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;

import java.util.List;
import java.util.Objects;

public class Utils {
    public static final String ACTION_STARRED_CARD_ADD = "added_starred_card";
    public static final String ACTION_JOIN_GAME = "joined_game";
    public static final String ACTION_SPECTATE_GAME = "spectate_game";
    public static final String ACTION_LEFT_GAME = "left_game";
    public static final String ACTION_SKIP_TUTORIAL = "skipped_tutorial";
    public static final String ACTION_DONE_TUTORIAL = "did_tutorial";
    public static final String ACTION_SENT_GAME_MSG = "sent_message_game";
    public static final String ACTION_ADDED_CARDCAST = "added_cardcast_deck";
    public static final String ACTION_JUDGE_CARD = "judged_card";
    public static final String ACTION_PLAY_CUSTOM_CARD = "played_custom_card";
    public static final String ACTION_PLAY_CARD = "played_card";
    public static final String ACTION_STARRED_DECK_ADD = "added_starred_deck";
    public static final String ACTION_SENT_MSG = "sent_message";

    @NonNull
    public static String composeCardcastDeckSentence(CardcastCard blackCard, CardcastCard whiteCard) {
        StringBuilder builder = new StringBuilder();
        builder.append(blackCard.text.get(0));
        builder.append("<u>").append(whiteCard.text.get(0)).append("</u>");
        if (blackCard.text.size() > 1) builder.append(blackCard.text.get(1));

        return builder.toString();
    }

    @Nullable
    public static Game findGame(List<Game> games, int gid) {
        for (Game game : games)
            if (game.gid == gid)
                return game;

        return null;
    }

    @Nullable
    public static CardSet findCardSet(List<CardSet> sets, int id) {
        for (CardSet set : sets)
            if (Objects.equals(set.id, id))
                return set;

        return null;
    }

    @NonNull
    public static TapTarget tapTargetForView(View view, @StringRes int title, @StringRes int desc) {
        Context ctx = view.getContext();
        return TapTarget.forView(view, ctx.getString(title), ctx.getString(desc)).transparentTarget(view instanceof FloatingActionButton);
    }

    public static int indexOf(List<GameInfo.Player> players, String nick) {
        for (int i = 0; i < players.size(); i++)
            if (Objects.equals(players.get(i).name, nick)) return i;

        return -1;
    }

    public static int indexOf(List<Game> games, int gid) {
        for (int i = 0; i < games.size(); i++)
            if (games.get(i).gid == gid) return i;

        return -1;
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
        public static final Toaster.Message FAILED_CREATING_GAME = new Toaster.Message(R.string.failedCreatingGame, true);
        public static final Toaster.Message FAILED_START_GAME = new Toaster.Message(R.string.failedStartGame, true);
        public static final Toaster.Message GAME_STARTED = new Toaster.Message(R.string.gameStarted, false);
        public static final Toaster.Message OPTIONS_CHANGED = new Toaster.Message(R.string.optionsChanged, false);
        public static final Toaster.Message FAILED_CHANGING_OPTIONS = new Toaster.Message(R.string.failedChangingOptions, true);
        public static final Toaster.Message CARDCAST_ADDED = new Toaster.Message(R.string.cardcastAdded, false);
        public static final Toaster.Message FAILED_ADDING_CARDCAST = new Toaster.Message(R.string.failedAddingCardcast, true);
        public static final Toaster.Message NOT_ENOUGH_PLAYERS = new Toaster.Message(R.string.notEnoughPlayers, false);
        public static final Toaster.Message INVALID_CARDCAST_CODE = new Toaster.Message(R.string.invalidCardcastCode, false);
        public static final Toaster.Message NO_STARRED_DECKS = new Toaster.Message(R.string.noStarredDecks, false);
        public static final Toaster.Message ADDED_STARRED_DECKS = new Toaster.Message(R.string.starredDecksAdded, false);
        public static final Toaster.Message PARTIAL_ADD_STARRED_DECKS_FAILED = new Toaster.Message(R.string.addStarredDecksFailedPartial, true);
        public static final Toaster.Message FAILED_PLAYING = new Toaster.Message(R.string.failedPlayingCard, true);
        public static final Toaster.Message FAILED_JUDGING = new Toaster.Message(R.string.failedJudging, true);
        public static final Toaster.Message STARRED_CARD = new Toaster.Message(R.string.addedCardToStarred, false);
        public static final Toaster.Message CARDSET_REMOVED = new Toaster.Message(R.string.cardcastDeckRemoved, false);
        public static final Toaster.Message FAILED_REMOVING_CARDSET = new Toaster.Message(R.string.failedRemovingCardcastDeck, true);
        public static final Toaster.Message FAILED_ADDING_SERVER = new Toaster.Message(R.string.failedAddingServer, true);
    }
}
