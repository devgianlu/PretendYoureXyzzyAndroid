package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.gianlu.commonutils.Adapters.Filterable;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class Game implements Filterable<Game.Protection>, Serializable {
    public final int gid;
    final ArrayList<String> players;
    final ArrayList<String> spectators;
    private final boolean hasPassword;
    private String host;
    private Options options;
    private Status status;

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

    public synchronized int playersSize() {
        return players.size();
    }

    public synchronized boolean hasSpectator(String name) {
        return spectators.contains(name);
    }

    public synchronized String getHost() {
        return host;
    }

    public synchronized void setHost(String host) {
        this.host = host;
    }

    public synchronized boolean isStatus(Game.Status status) {
        return this.status == status;
    }

    public synchronized Options getOptions() {
        return options;
    }

    public synchronized void setOptions(Options options) {
        this.options = options;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public Protection getFilterable() {
        return hasPassword(false) ? Protection.LOCKED : Protection.OPEN;
    }

    public boolean hasPassword(boolean knowsPassword) {
        if (knowsPassword) return options.password != null && !options.password.isEmpty();
        else return hasPassword;
    }

    public synchronized int spectatorsSize() {
        return spectators.size();
    }

    public synchronized String getSpectatorsStringList() {
        return CommonUtils.join(spectators, ", ");
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
        private static final int BL_MIN = 0;
        private static final int BL_MAX = 30;
        private static final int VL_PL_MAX = 20;
        private static final int VL_MIN = 0;
        private static final int PL_MIN = 3;
        private static final int SL_MAX = 69;
        private static final int SL_MIN = 4;
        public final String timerMultiplier;
        public final int spectatorsLimit;
        public final int playersLimit;
        public final int scoreLimit;
        public final int blanksLimit;
        public final ArrayList<Integer> cardSets;
        public final String password;

        public Options(JSONObject obj) throws JSONException {
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

        public static Options validateAndCreate(String timerMultiplier, String spectatorsLimit, String playersLimit, String scoreLimit, String blanksLimit, LinearLayout cardSets, String password) throws InvalidFieldException {
            if (!CommonUtils.contains(VALID_TIMER_MULTIPLIERS, timerMultiplier))
                throw new InvalidFieldException(R.id.gameOptions_timerMultiplier, R.string.invalidTimerMultiplier);

            int vL = parseIntOrThrow(spectatorsLimit, R.id.editGameOptions_spectatorLimit);
            checkMaxMin(vL, VL_MIN, VL_PL_MAX, R.id.editGameOptions_spectatorLimit);

            int pL = parseIntOrThrow(playersLimit, R.id.editGameOptions_playerLimit);
            checkMaxMin(pL, PL_MIN, VL_PL_MAX, R.id.editGameOptions_playerLimit);

            int sl = parseIntOrThrow(scoreLimit, R.id.editGameOptions_scoreLimit);
            checkMaxMin(sl, SL_MIN, SL_MAX, R.id.editGameOptions_scoreLimit);

            int bl = parseIntOrThrow(blanksLimit, R.id.editGameOptions_blankCards);
            checkMaxMin(bl, BL_MIN, BL_MAX, R.id.editGameOptions_blankCards);

            ArrayList<Integer> cardSetIds = new ArrayList<>();
            for (int i = 0; i < cardSets.getChildCount(); i++) {
                View view = cardSets.getChildAt(i);
                if (view instanceof CheckBox && ((CheckBox) view).isChecked())
                    cardSetIds.add(((Deck) view.getTag()).id);
            }

            return new Game.Options(timerMultiplier, vL, pL, sl, bl, cardSetIds, password);
        }

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
