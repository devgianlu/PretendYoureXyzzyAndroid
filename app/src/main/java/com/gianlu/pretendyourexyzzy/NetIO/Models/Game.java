package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public class Game implements Filterable<Game.Protection>, Serializable {
    public final int gid;
    public final ArrayList<String> players;
    public final ArrayList<String> spectators;
    public final boolean hasPassword;
    public final String host;
    public final Options options;
    public final Status status;

    public Game(JSONObject obj) throws JSONException {
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
    public Protection getFilterable() {
        return hasPassword(false) ? Protection.LOCKED : Protection.OPEN;
    }

    public boolean hasPassword(boolean knowsPassword) {
        if (knowsPassword) return options.password != null && !options.password.isEmpty();
        else return hasPassword;
    }

    public enum Protection {
        LOCKED,
        OPEN
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

            throw new IllegalArgumentException("Cannot find status with value: " + val);
        }

        public boolean isStarted() {
            return this != LOBBY;
        }
    }

    public static class NameComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o1.host.compareToIgnoreCase(o2.host);
        }
    }

    public static class NumPlayersComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o2.players.size() - o1.players.size();
        }
    }

    public static class NumSpectatorsComparator implements Comparator<Game> {

        @Override
        public int compare(Game o1, Game o2) {
            return o2.spectators.size() - o1.spectators.size();
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
            password = obj.optString("pw", null);

            JSONArray cardsSetsArray = obj.getJSONArray("css");
            cardSets = new ArrayList<>();
            for (int i = 0; i < cardsSetsArray.length(); i++)
                cardSets.add(cardsSetsArray.getInt(i));
        }

        Options(String timerMultiplier, int spectatorsLimit, int playersLimit, int scoreLimit, int blanksLimit, ArrayList<Integer> cardSets, @Nullable String password) {
            this.timerMultiplier = timerMultiplier;
            this.spectatorsLimit = spectatorsLimit;
            this.playersLimit = playersLimit;
            this.scoreLimit = scoreLimit;
            this.blanksLimit = blanksLimit;
            this.cardSets = cardSets;
            this.password = password;
        }

        public static int timerMultiplierIndex(String timerMultiplier) {
            int index = CommonUtils.indexOf(VALID_TIMER_MULTIPLIERS, timerMultiplier);
            if (index == -1) index = 3; // 1x
            return index;
        }

        private static int parseIntOrThrow(String val, @IdRes int fieldId) throws InvalidFieldException {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ex) {
                throw new InvalidFieldException(fieldId, R.string.invalidNumber);
            }
        }

        private static void checkMaxMin(int val, int min, int max, @IdRes int fieldId) throws InvalidFieldException {
            if (val < min || val > max)
                throw new InvalidFieldException(fieldId, min, max);
        }

        @NonNull
        public static Options validateAndCreate(@NonNull Pyx.Server.Params params, String timerMultiplier, String spectatorsLimit,
                                                String playersLimit, String scoreLimit, String blanksLimit, LinearLayout cardSets,
                                                String password) throws InvalidFieldException {
            if (!CommonUtils.contains(VALID_TIMER_MULTIPLIERS, timerMultiplier))
                throw new InvalidFieldException(R.id.gameOptions_timerMultiplier, R.string.invalidTimerMultiplier);

            int vL = parseIntOrThrow(spectatorsLimit, R.id.editGameOptions_spectatorLimit);
            checkMaxMin(vL, params.spectatorsMin, params.spectatorsMax, R.id.editGameOptions_spectatorLimit);

            int pL = parseIntOrThrow(playersLimit, R.id.editGameOptions_playerLimit);
            checkMaxMin(pL, params.playersMin, params.playersMax, R.id.editGameOptions_playerLimit);

            int sl = parseIntOrThrow(scoreLimit, R.id.editGameOptions_scoreLimit);
            checkMaxMin(sl, params.scoreMin, params.scoreMax, R.id.editGameOptions_scoreLimit);

            int bl = parseIntOrThrow(blanksLimit, R.id.editGameOptions_blankCards);
            checkMaxMin(bl, params.blankCardsMin, params.blankCardsMax, R.id.editGameOptions_blankCards);

            ArrayList<Integer> cardSetIds = new ArrayList<>();
            for (int i = 0; i < cardSets.getChildCount(); i++) {
                View view = cardSets.getChildAt(i);
                if (view instanceof CheckBox && ((CheckBox) view).isChecked())
                    cardSetIds.add(((Deck) view.getTag()).id);
            }

            return new Game.Options(timerMultiplier, vL, pL, sl, bl, cardSetIds, password);
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
            return password != null ? password.equals(options.password) : options.password == null;
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

        public static class InvalidFieldException extends Throwable {
            public final int fieldId;
            public final int throwMessage;
            public final int min;
            public final int max;

            InvalidFieldException(@IdRes int fieldId, @StringRes int throwMessage) {
                this.fieldId = fieldId;
                this.throwMessage = throwMessage;
                this.min = -1;
                this.max = -1;
            }

            InvalidFieldException(@IdRes int fieldId, int min, int max) {
                this.fieldId = fieldId;
                this.throwMessage = R.string.outOfRange;
                this.min = min;
                this.max = max;
            }
        }
    }
}
