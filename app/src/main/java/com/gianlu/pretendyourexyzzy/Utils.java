package com.gianlu.pretendyourexyzzy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.NetIO.Models.CardcastCard;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Deck;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Game;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GameInfo;
import com.gianlu.pretendyourexyzzy.NetIO.NameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Utils {
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
    public static final String ACTION_SHOW_ROUND = "show_round";
    public static final String ACTION_SAVE_SHARE_ROUND = "save_share_round";
    public static final String ACTION_SENT_MSG = "sent_message";
    public static final String ACTION_OPEN_URBAN_DICT = "opened_urban_dict_sheet";
    public static final String ACTION_UNKNOWN_EVENT = "unknown_server_event";

    private Utils() {
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
