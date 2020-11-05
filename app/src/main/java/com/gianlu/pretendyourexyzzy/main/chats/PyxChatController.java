package com.gianlu.pretendyourexyzzy.main.chats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.pretendyourexyzzy.BlockedUsers;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxRequests;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;
import com.gianlu.pretendyourexyzzy.dialogs.NewUserInfoDialog;
import com.google.android.gms.tasks.Task;

import java.util.List;

class PyxChatController {
    private final int gid;
    private RegisteredPyx pyx;
    private Pyx.OnEventListener eventListener;

    private PyxChatController(int gid) {
        this.gid = gid;
    }

    @NonNull
    static PyxChatController globalController() {
        return new PyxChatController(-1);
    }

    @NonNull
    static PyxChatController gameController(int gid) {
        return new PyxChatController(gid);
    }

    @Nullable
    String username() {
        return pyx == null ? null : pyx.user().nickname;
    }

    @NonNull
    List<PollMessage> init() throws LevelMismatchException {
        pyx = RegisteredPyx.get();
        return gid == -1 ? pyx.chat().getMessagesForGlobal() : pyx.chat().getMessagesForGame(gid);
    }

    void listener(@NonNull Listener listener) {
        if (pyx == null) throw new IllegalStateException();

        pyx.polling().addListener(eventListener = msg -> {
            if (msg.sender == null || msg.message == null || msg.event != PollMessage.Event.CHAT)
                return;

            if ((msg.gid == -1 && gid == -1) || (gid != -1 && msg.gid == gid)) {
                if (!msg.wall && BlockedUsers.isBlocked(msg.sender))
                    return;

                listener.onChatMessage(msg);
            }
        });
    }

    void send(@NonNull String msg, @NonNull SendCallback callback) {
        if (pyx == null) throw new IllegalStateException();

        boolean emote;
        boolean wall;
        if (msg.startsWith("/")) {
            String[] split = msg.split(" ");
            if (split.length == 1 || split[1].isEmpty())
                return;

            msg = split[1];

            switch (split[0].substring(1)) {
                case "me":
                    emote = true;
                    wall = false;
                    break;
                case "wall":
                    emote = false;
                    wall = true;
                    break;
                default:
                    callback.unknownCommand();
                    return;
            }
        } else {
            emote = false;
            wall = false;
        }

        send(msg, emote, wall)
                .addOnSuccessListener(aVoid -> callback.onSuccessful())
                .addOnFailureListener(callback::onFailed);
    }

    @NonNull
    private Task<Void> send(@NonNull String msg, boolean emote, boolean wall) {
        if (gid == -1) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_SENT_MSG);
            return pyx.request(PyxRequests.sendMessage(msg, emote, wall));
        } else {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_SENT_GAME_MSG);
            return pyx.request(PyxRequests.sendGameMessage(gid, msg, emote));
        }
    }

    void onDestroy() {
        if (pyx != null && eventListener != null) pyx.polling().removeListener(eventListener);
    }

    void showUserInfo(@NonNull FragmentActivity activity, @NonNull String sender) {
        NewUserInfoDialog.get(sender, true).show(activity.getSupportFragmentManager(), null);
    }

    void readAllMessages(long timestamp) {
        if (pyx == null) return;

        if (gid == -1) pyx.chat().resetGlobalUnread(timestamp);
        else pyx.chat().resetGameUnread(timestamp);
    }

    interface SendCallback {
        void onSuccessful();

        void unknownCommand();

        void onFailed(@NonNull Exception ex);
    }

    interface Listener {
        void onChatMessage(@NonNull PollMessage msg);
    }
}
