package com.gianlu.pretendyourexyzzy.api;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.api.models.Game;
import com.gianlu.pretendyourexyzzy.api.models.GameCards;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.GamesList;
import com.gianlu.pretendyourexyzzy.api.models.Name;
import com.gianlu.pretendyourexyzzy.api.models.User;
import com.gianlu.pretendyourexyzzy.api.models.WhoisResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Response;

public final class PyxRequests {
    private static final Pyx.Processor<FirstLoad> FIRST_LOAD_PROCESSOR = (response, obj) -> {
        User user = null;
        if (obj.getBoolean("ip") && obj.has("n")) {
            String lastSessionId = Prefs.getString(PK.LAST_JSESSIONID, null);
            if (lastSessionId != null) user = new User(lastSessionId, obj);
        }

        return new FirstLoad(obj, user);
    };
    private static final Pyx.Processor<User> REGISTER_PROCESSOR = (response, obj) -> {
        String sessionId = findSessionId(response);
        if (sessionId == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                throw new JSONException(new NullPointerException("Cannot find cookie JSESSIONID!"));
            else
                throw new JSONException("Cannot find cookie JSESSIONID!");
        }

        return new User(sessionId, obj);
    };
    private static final Pyx.Processor<GamePermalink> CREATE_GAME_PROCESSOR = (response, obj) -> new GamePermalink(obj);
    private static final Pyx.Processor<GameInfo> GAME_INFO_PROCESSOR = (response, obj) -> new GameInfo(obj);
    private static final Pyx.Processor<List<Name>> NAMES_LIST_PROCESSOR = (response, obj) -> {
        JSONArray array = obj.getJSONArray("nl");
        List<Name> names = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) names.add(new Name(array.getString(i)));
        return names;
    };
    private static final Pyx.Processor<GameCards> GAME_CARDS_PROCESSOR = (response, obj) -> new GameCards(obj);
    private static final Pyx.Processor<GamesList> GAMES_LIST_PROCESSOR = (response, obj) -> {
        JSONArray array = obj.getJSONArray("gl");
        final GamesList games = new GamesList(obj.getInt("mg"));
        for (int i = 0; i < array.length(); i++)
            games.add(new Game(array.getJSONObject(i)));
        return games;
    };
    private static final Pyx.Processor<WhoisResult> WHOIS_RESULT_PROCESSOR = (response, obj) -> new WhoisResult(obj);
    private static final String TAG = PyxRequests.class.getSimpleName();

    @Nullable
    private static String findSessionId(@NonNull Response response) {
        for (String cookie : response.headers("Set-Cookie")) {
            String[] segments = cookie.split(";");
            String[] keyValue = segments[0].split("=");
            if (Objects.equals(keyValue[0], "JSESSIONID"))
                return keyValue[1];
        }

        return null;
    }

    @NonNull
    public static PyxRequestWithResult<FirstLoad> firstLoad() {
        return new PyxRequestWithResult<>(Pyx.Op.FIRST_LOAD, FIRST_LOAD_PROCESSOR);
    }

    @NonNull
    public static PyxRequest logout() {
        return new PyxRequest(Pyx.Op.LOGOUT);
    }

    @NonNull
    public static PyxRequestWithResult<User> register(@NonNull String nickname, @Nullable String idCode, @Nullable String pid) {
        return new PyxRequestWithResult<>(Pyx.Op.REGISTER, REGISTER_PROCESSOR,
                new PyxRequest.Param("n", nickname),
                new PyxRequest.Param("idc", idCode),
                new PyxRequest.Param("pid", pid));
    }

    @NonNull
    public static PyxRequest leaveGame(int gid) {
        return new PyxRequest(Pyx.Op.LEAVE_GAME, new PyxRequest.Param("gid", String.valueOf(gid)));
    }

    public static PyxRequest changeGameOptions(int gid, @NonNull Game.Options options) throws JSONException {
        return new PyxRequest(Pyx.Op.CHANGE_GAME_OPTIONS,
                new PyxRequest.Param("gid", String.valueOf(gid)),
                new PyxRequest.Param("go", options.toJson().toString()));
    }

    @NonNull
    public static PyxRequestWithResult<GamePermalink> createGame() {
        return new PyxRequestWithResult<>(Pyx.Op.CREATE_GAME, CREATE_GAME_PROCESSOR);
    }

    @NonNull
    public static PyxRequestWithResult<GamePermalink> joinGame(final int gid, @Nullable String password) {
        return new PyxRequestWithResult<>(Pyx.Op.JOIN_GAME, (response, obj) -> new GamePermalink(gid, obj), new PyxRequest.Param("gid", String.valueOf(gid)), new PyxRequest.Param("pw", password));
    }

    @NonNull
    public static PyxRequestWithResult<GamePermalink> spectateGame(final int gid, @Nullable String password) {
        return new PyxRequestWithResult<>(Pyx.Op.SPECTATE_GAME, (response, obj) -> new GamePermalink(gid, obj), new PyxRequest.Param("gid", String.valueOf(gid)), new PyxRequest.Param("pw", password));
    }

    @NonNull
    public static PyxRequestWithResult<GameInfo> getGameInfo(int gid) {
        return new PyxRequestWithResult<>(Pyx.Op.GET_GAME_INFO, GAME_INFO_PROCESSOR, new PyxRequest.Param("gid", String.valueOf(gid)));
    }

    @NonNull
    public static PyxRequest sendGameMessage(int gid, @NonNull String msg, boolean emote) {
        return new PyxRequest(Pyx.Op.GAME_CHAT, new PyxRequest.Param("gid", String.valueOf(gid)), new PyxRequest.Param("m", msg),
                new PyxRequest.Param("me", String.valueOf(emote)));
    }

    @NonNull
    public static PyxRequest sendMessage(@NonNull String msg, boolean emote, boolean wall) {
        return new PyxRequest(Pyx.Op.CHAT, new PyxRequest.Param("m", msg), new PyxRequest.Param("me", String.valueOf(emote)),
                new PyxRequest.Param("wall", String.valueOf(wall)));
    }

    @NonNull
    public static PyxRequestWithResult<List<Name>> getNamesList() {
        return new PyxRequestWithResult<>(Pyx.Op.GET_NAMES_LIST, NAMES_LIST_PROCESSOR);
    }

    @NonNull
    public static PyxRequest removeCardcastDeck(int gid, @NonNull String code) {
        return new PyxRequest(Pyx.Op.REMOVE_CARDCAST_CARD_SET,
                new PyxRequest.Param("gid", String.valueOf(gid)),
                new PyxRequest.Param("cci", code));
    }

    @NonNull
    public static PyxRequestWithResult<GameCards> getGameCards(int gid) {
        return new PyxRequestWithResult<>(Pyx.Op.GET_GAME_CARDS, GAME_CARDS_PROCESSOR, new PyxRequest.Param("gid", String.valueOf(gid)));
    }

    @NonNull
    public static PyxRequest startGame(int gid) {
        return new PyxRequest(Pyx.Op.START_GAME, new PyxRequest.Param("gid", String.valueOf(gid)));
    }

    @NonNull
    public static PyxRequest judgeCard(int gid, int cardId) {
        return new PyxRequest(Pyx.Op.JUDGE_SELECT,
                new PyxRequest.Param("gid", String.valueOf(gid)),
                new PyxRequest.Param("cid", String.valueOf(cardId)));
    }

    @NonNull
    public static PyxRequest playCard(int gid, int cardId, @Nullable String customText) {
        return new PyxRequest(Pyx.Op.PLAY_CARD,
                new PyxRequest.Param("gid", String.valueOf(gid)),
                new PyxRequest.Param("cid", String.valueOf(cardId)),
                new PyxRequest.Param("m", customText));
    }

    @NonNull
    public static PyxRequestWithResult<GamesList> getGamesList() {
        return new PyxRequestWithResult<>(Pyx.Op.GET_GAMES_LIST, GAMES_LIST_PROCESSOR);
    }

    @NonNull
    public static PyxRequestWithResult<WhoisResult> whois(String name) {
        return new PyxRequestWithResult<>(Pyx.Op.WHOIS, WHOIS_RESULT_PROCESSOR, new PyxRequest.Param("n", name));
    }

}
