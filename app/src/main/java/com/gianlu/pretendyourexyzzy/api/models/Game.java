package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.Filterable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Game implements Filterable<Game.Filters>, Serializable {
    public final int gid;
    public final ArrayList<String> players;
    public final ArrayList<String> spectators;
    public final boolean hasPassword;
    public final String host;
    public final Options options;
    public final Status status;
    public final List<Deck> customDecks;

    public Game(@NonNull JSONObject obj) throws JSONException {
        host = obj.getString("H");
        gid = obj.getInt("gid");
        status = Status.parse(obj.getString("S"));
        options = new Options(obj.getJSONObject("go"));
        hasPassword = obj.getBoolean("hp");

        JSONArray playersArray = obj.getJSONArray("P");
        players = new ArrayList<>();
        for (int i = 0; i < playersArray.length(); i++)
            players.add(playersArray.getString(i));

        JSONArray spectatorsArray = obj.getJSONArray("V");
        spectators = new ArrayList<>();
        for (int i = 0; i < spectatorsArray.length(); i++) {
            String name = spectatorsArray.getString(i);
            if (!spectators.contains(name)) spectators.add(name);
        }

        JSONArray customDecksArray = obj.optJSONArray("ccs");
        if (customDecksArray != null) {
            customDecks = new ArrayList<>(customDecksArray.length());
            for (int i = 0; i < customDecksArray.length(); i++)
                customDecks.add(new Deck(customDecksArray.getJSONObject(i)));
        } else {
            customDecks = new ArrayList<>();
        }
    }

    public static int indexOf(@NonNull List<Game> games, int gid) {
        for (int i = 0; i < games.size(); i++)
            if (games.get(i).gid == gid) return i;

        return -1;
    }

    @Nullable
    public static Game findGame(@NonNull List<Game> games, int gid) {
        for (Game game : games)
            if (game.gid == gid)
                return game;

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Game game = (Game) o;
        if (gid != game.gid) return false;
        if (hasPassword != game.hasPassword) return false;
        if (!players.equals(game.players)) return false;
        if (!spectators.equals(game.spectators)) return false;
        if (!host.equals(game.host)) return false;
        if (!options.equals(game.options)) return false;
        return status == game.status;
    }

    @Override
    public int hashCode() {
        int result = gid;
        result = 31 * result + players.hashCode();
        result = 31 * result + spectators.hashCode();
        result = 31 * result + (hasPassword ? 1 : 0);
        result = 31 * result + host.hashCode();
        result = 31 * result + options.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }

    @Override
    public Filters[] getMatchingFilters() {
        return new Filters[]{
                hasPassword(false) ? Filters.LOCKED : Filters.OPEN,
                status.isStarted() ? Filters.IN_PROGRESS : Filters.LOBBY
        };
    }

    public boolean hasPassword(boolean knowsPassword) {
        if (knowsPassword) return options.password != null && !options.password.isEmpty();
        else return hasPassword;
    }

    public enum Filters {
        LOCKED, OPEN,
        LOBBY, IN_PROGRESS
    }

    public enum Status {
        DEALING("d"),
        ROUND_OVER("ro"),
        JUDGING("j"),
        LOBBY("l"),
        PLAYING("p");

        public final String val;

        Status(String val) {
            this.val = val;
        }

        public static Status parse(String val) {
            for (Status status : values())
                if (Objects.equals(status.val, val))
                    return status;

            throw new IllegalArgumentException("Cannot find purchaseStatus with value: " + val);
        }

        public boolean isStarted() {
            return this != LOBBY;
        }
    }

    public final static class NameComparator implements Comparator<Game> {

        @Override
        public int compare(@NonNull Game o1, @NonNull Game o2) {
            return o1.host.compareToIgnoreCase(o2.host);
        }
    }

    public final static class NumAvailablePlayersComparator implements Comparator<Game> {

        @Override
        public int compare(@NonNull Game o1, @NonNull Game o2) {
            return (o2.options.playersLimit - o2.players.size()) - (o1.options.playersLimit - o1.players.size());
        }
    }

    public final static class NumAvailableSpectatorsComparator implements Comparator<Game> {

        @Override
        public int compare(@NonNull Game o1, @NonNull Game o2) {
            return (o2.options.spectatorsLimit - o2.spectators.size()) - (o1.options.spectatorsLimit - o1.spectators.size());
        }
    }

    public static class Options implements Serializable {
        public static final String[] VALID_TIMER_MULTIPLIERS = {"0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x", "2.5x", "3x", "4x", "5x", "10x", "Unlimited"};
        public final String timerMultiplier;
        public final int spectatorsLimit;
        public final int playersLimit;
        public final int scoreLimit;
        public final int blanksLimit;
        public final ArrayList<Integer> cardSets;
        public final String password;

        Options(@NonNull JSONObject obj) throws JSONException {
            timerMultiplier = obj.getString("tm");
            spectatorsLimit = obj.getInt("vL");
            playersLimit = obj.getInt("pL");
            scoreLimit = obj.getInt("sl");
            blanksLimit = obj.getInt("bl");
            password = CommonUtils.optString(obj, "pw");

            JSONArray cardsSetsArray = obj.getJSONArray("css");
            cardSets = new ArrayList<>();
            for (int i = 0; i < cardsSetsArray.length(); i++)
                cardSets.add(cardsSetsArray.getInt(i));
        }

        /**
         * Creates a new instance of the options, assuming fields have been validated.
         */
        public Options(String timerMultiplier, int spectatorsLimit, int playersLimit, int scoreLimit, int blanksLimit, ArrayList<Integer> cardSets, @Nullable String password) {
            this.timerMultiplier = timerMultiplier;
            this.spectatorsLimit = spectatorsLimit;
            this.playersLimit = playersLimit;
            this.scoreLimit = scoreLimit;
            this.blanksLimit = blanksLimit;
            this.cardSets = cardSets;
            this.password = password;
        }

        public static int timerMultiplierIndex(@NotNull String timerMultiplier) {
            int index = CommonUtils.indexOf(VALID_TIMER_MULTIPLIERS, timerMultiplier);
            if (index == -1) index = 3; // 1x
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Options options = (Options) o;
            if (spectatorsLimit != options.spectatorsLimit) return false;
            if (playersLimit != options.playersLimit) return false;
            if (scoreLimit != options.scoreLimit) return false;
            if (blanksLimit != options.blanksLimit) return false;
            if (!timerMultiplier.equals(options.timerMultiplier)) return false;
            if (!cardSets.equals(options.cardSets)) return false;
            return Objects.equals(password, options.password);
        }

        @Override
        public int hashCode() {
            int result = timerMultiplier.hashCode();
            result = 31 * result + spectatorsLimit;
            result = 31 * result + playersLimit;
            result = 31 * result + scoreLimit;
            result = 31 * result + blanksLimit;
            result = 31 * result + cardSets.hashCode();
            result = 31 * result + (password != null ? password.hashCode() : 0);
            return result;
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("css", CommonUtils.join(cardSets, ","))
                    .put("tm", timerMultiplier)
                    .put("vL", spectatorsLimit)
                    .put("pL", playersLimit)
                    .put("sl", scoreLimit)
                    .put("bl", blanksLimit)
                    .put("pw", password);
        }
    }
}
