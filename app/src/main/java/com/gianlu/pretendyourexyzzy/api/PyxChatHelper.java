package com.gianlu.pretendyourexyzzy.api;

import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.pretendyourexyzzy.api.models.PollMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PyxChatHelper {
    private final List<UnreadCountListener> unreadCountListeners = new ArrayList<>(3);
    private final Object countLock = new Object();
    private final LruCache<Integer, List<PollMessage>> storedMessages = new LruCache<Integer, List<PollMessage>>(128) {
        @Override
        protected int sizeOf(Integer key, @NonNull List<PollMessage> value) {
            return value.size();
        }
    };
    private final Handler handler;
    private volatile int gameUnreadCount = 0;
    private volatile long gameLastSeen = 0;
    private volatile int globalUnreadCount = 0;
    private volatile long globalLastSeen = 0;

    PyxChatHelper() {
        handler = new Handler(Looper.getMainLooper());
    }

    public void addUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.add(listener);
    }

    public void removeUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.remove(listener);
    }

    public int getGlobalUnread() {
        return globalUnreadCount;
    }

    public void resetGlobalUnread(long timestamp) {
        synchronized (countLock) {
            globalLastSeen = timestamp;
            globalUnreadCount = 0;
        }

        handler.post(() -> {
            for (UnreadCountListener listener : new ArrayList<>(unreadCountListeners))
                listener.pyxUnreadCountUpdated(globalUnreadCount, gameUnreadCount);
        });
    }

    public void resetGameUnread(long timestamp) {
        synchronized (countLock) {
            gameLastSeen = timestamp;
            gameUnreadCount = 0;
        }

        handler.post(() -> {
            for (UnreadCountListener listener : new ArrayList<>(unreadCountListeners))
                listener.pyxUnreadCountUpdated(globalUnreadCount, gameUnreadCount);
        });
    }

    @WorkerThread
    void handleChatEvent(@NonNull PollMessage msg) {
        if (msg.event != PollMessage.Event.CHAT) return;

        List<PollMessage> list = storedMessages.get(msg.gid);
        if (list == null) storedMessages.put(msg.gid, list = new ArrayList<>());
        list.add(msg);

        synchronized (countLock) {
            long lastSeen = msg.gid == -1 ? globalLastSeen : gameLastSeen;
            if (msg.timestamp > lastSeen) {
                if (msg.gid != -1) gameUnreadCount++;
                else globalUnreadCount++;

                handler.post(() -> {
                    for (UnreadCountListener listener : new ArrayList<>(unreadCountListeners))
                        listener.pyxUnreadCountUpdated(globalUnreadCount, gameUnreadCount);
                });
            }
        }
    }

    @NonNull
    private List<PollMessage> getMessages(int gid) {
        List<PollMessage> list = storedMessages.get(gid);
        if (list == null) return Collections.emptyList();
        else return list;
    }

    @NonNull
    public List<PollMessage> getMessagesForGame(int gid) {
        if (gid == -1) throw new IllegalArgumentException();
        return getMessages(gid);
    }

    @NonNull
    public List<PollMessage> getMessagesForGlobal() {
        return getMessages(-1);
    }

    @UiThread
    public interface UnreadCountListener {
        void pyxUnreadCountUpdated(int globalUnread, int gameUnread);
    }
}
