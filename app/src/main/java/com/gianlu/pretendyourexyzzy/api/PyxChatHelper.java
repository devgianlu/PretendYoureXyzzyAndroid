package com.gianlu.pretendyourexyzzy.api;

import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.pretendyourexyzzy.api.models.PollMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PyxChatHelper {
    private final List<UnreadCountListener> unreadCountListeners = new ArrayList<>(3);
    private final LifecycleAwareHandler handler;
    private final Object countLock = new Object();
    private final LruCache<Integer, List<PollMessage>> storedMessages = new LruCache<Integer, List<PollMessage>>(128) {
        @Override
        protected int sizeOf(Integer key, List<PollMessage> value) {
            return value.size();
        }
    };
    private volatile int gameUnreadCount = 0;
    private volatile long gameLastSeen = 0;
    private volatile int globalUnreadCount = 0;
    private volatile long globalLastSeen = 0;

    PyxChatHelper(@NonNull LifecycleAwareHandler handler) {
        this.handler = handler;
    }

    public void addUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.add(listener);
    }

    public void removeUnreadCountListener(@NonNull UnreadCountListener listener) {
        unreadCountListeners.remove(listener);
    }

    public void resetGlobalUnread(long timestamp) {
        int total;
        synchronized (countLock) {
            globalLastSeen = timestamp;
            globalUnreadCount = 0;
            total = globalUnreadCount + gameUnreadCount;
        }

        handler.post(null, () -> {
            for (UnreadCountListener listener : new ArrayList<>(unreadCountListeners))
                listener.onPyxUnread(total);
        });
    }

    public void resetGameUnread(long timestamp) {
        int total;
        synchronized (countLock) {
            gameLastSeen = timestamp;
            gameUnreadCount = 0;
            total = globalUnreadCount + gameUnreadCount;
        }

        handler.post(null, () -> {
            for (UnreadCountListener listener : new ArrayList<>(unreadCountListeners))
                listener.onPyxUnread(total);
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
                int total;
                if (msg.gid != -1) gameUnreadCount++;
                else globalUnreadCount++;
                total = globalUnreadCount + gameUnreadCount;

                handler.post(null, () -> {
                    for (UnreadCountListener listener : new ArrayList<>(unreadCountListeners))
                        listener.onPyxUnread(total);
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
        void onPyxUnread(int count);
    }
}
