package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.getkeepsafe.taptargetview.TapTarget;
import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Deck;
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
    public static Deck findCardSet(List<Deck> sets, int id) {
        for (Deck set : sets)
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
}
