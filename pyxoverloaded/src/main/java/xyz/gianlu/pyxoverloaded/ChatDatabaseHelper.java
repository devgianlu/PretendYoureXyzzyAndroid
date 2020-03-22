package xyz.gianlu.pyxoverloaded;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.ChatMessage;
import xyz.gianlu.pyxoverloaded.model.ChatMessages;

class ChatDatabaseHelper extends SQLiteOpenHelper {
    private final Map<String, Chat> chatsCache = new HashMap<>(64);
    private final Map<String, Long> lastSeenCache = new HashMap<>(64);
    private final Map<String, ChatMessage> lastMessageCache = new HashMap<>(64);

    ChatDatabaseHelper(Context context) {
        super(context, "chat.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats(id TEXT UNIQUE NOT NULL, oneParticipant TEXT NOT NULL, otherParticipant TEXT NOT NULL, last_seen LONG DEFAULT NULL, last_msg TEXT DEFAULT NULL)");
        db.execSQL("CREATE TABLE messages(id TEXT UNIQUE NOT NULL, chat_id TEXT NOT NULL, text TEXT NOT NULL, timestamp LONG NOT NULL, `from` TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DELETE FROM chats");
        db.execSQL("DELETE FROM messages");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Nullable
    synchronized ChatMessage getMessage(@NonNull String chatId, @NonNull String msgId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM messages WHERE chat_id=? AND id=?", new String[]{chatId, msgId})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            return new ChatMessage(cursor);
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized ChatMessages getMessages(@NonNull String chatId, int limit, int offset) {
        Chat chat = getChat(chatId);
        if (chat == null) return null;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("messages", null, "chat_id=?", new String[]{chatId}, null, null, "timestamp DESC", String.valueOf(offset + limit))) {
            if (cursor == null)
                return null;

            if (!cursor.moveToFirst()) return null;
            if (offset != 0 && !cursor.move(offset))
                return null;

            ChatMessages list = new ChatMessages(limit, chat);
            do {
                list.add(new ChatMessage(cursor));
            } while (cursor.moveToNext());
            return list;
        } finally {
            db.endTransaction();
        }
    }

    synchronized void addMessage(@NonNull String chatId, @NonNull ChatMessage msg) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("id", msg.id);
            values.put("chat_id", chatId);
            values.put("text", msg.text);
            values.put("timestamp", msg.timestamp);
            values.put("`from`", msg.from);
            db.insert("messages", null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    synchronized void putMessages(@NonNull ChatMessages list) {
        if (list.isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String chatId = list.chat.id;
            for (ChatMessage msg : list) {
                ContentValues values = new ContentValues();
                values.put("id", msg.id);
                values.put("chat_id", chatId);
                values.put("text", msg.text);
                values.put("`timestamp`", msg.timestamp);
                values.put("`from`", msg.from);
                db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Chat getChat(@NonNull String chatId) {
        Chat chat = chatsCache.get(chatId);
        if (chat != null) return chat;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, "id=?", new String[]{chatId}, null, null, null, null)) {
            if (cursor == null || !cursor.moveToNext()) return null;

            chat = new Chat(cursor);
            chatsCache.put(chatId, chat);
            return chat;
        } finally {
            db.endTransaction();
        }
    }

    synchronized void putChat(@NonNull Chat chat) {
        if (chat.participants.size() != 2)
            throw new IllegalStateException();

        if (chatsCache.containsKey(chat.id)) return;
        else chatsCache.put(chat.id, chat);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("id", chat.id);
            values.put("oneParticipant", chat.participants.get(0));
            values.put("otherParticipant", chat.participants.get(1));
            db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Chat findChatWith(@NotNull String username) {
        for (Chat chat : chatsCache.values())
            if (chat.participants.contains(username))
                return chat;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, "oneParticipant=? OR otherParticipant=?", new String[]{username, username}, null, null, null, "1")) {
            if (cursor == null || !cursor.moveToNext())
                return null;

            Chat chat = new Chat(cursor);
            chatsCache.put(chat.id, chat);
            return chat;
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    synchronized List<Chat> getChats() {
        if (!chatsCache.isEmpty()) return new ArrayList<>(chatsCache.values());

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, null, null, null, null, null, null)) {
            if (cursor == null || !cursor.moveToNext())
                return Collections.emptyList();

            List<Chat> list = new ArrayList<>(32);
            do {
                Chat chat = new Chat(cursor);
                if (!chatsCache.containsKey(chat.id)) chatsCache.put(chat.id, chat);
                list.add(chat);
            } while (cursor.moveToNext());

            return list;
        } finally {
            db.endTransaction();
        }
    }

    synchronized void updateLastSeen(@NotNull String chatId, long lastSeen) {
        lastSeenCache.put(chatId, lastSeen);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("last_seen", lastSeen);
            db.update("chats", values, "id=?", new String[]{chatId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Long getLastSeen(@NonNull String chatId) {
        Long lastSeen = lastSeenCache.get(chatId);
        if (lastSeen != null) return lastSeen;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT last_seen FROM chats WHERE id=?", new String[]{chatId})) {
            if (!cursor.moveToNext()) return null;

            if (cursor.isNull(0))
                return null;

            lastSeen = cursor.getLong(0);
            lastSeenCache.put(chatId, lastSeen);
            return lastSeen;
        } finally {
            db.endTransaction();
        }
    }

    synchronized int countSinceLastSeen(@NonNull String chatId) {
        Long lastSeen = getLastSeen(chatId);
        if (lastSeen == null) return 0;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE timestamp >= ? AND chat_id=?", new String[]{String.valueOf(lastSeen), chatId})) {
            if (!cursor.moveToNext()) return 0;
            return cursor.getInt(0);
        } finally {
            db.endTransaction();
        }
    }

    int countTotalUnread() {
        int count = 0;
        for (Chat chat : getChats()) count += countSinceLastSeen(chat.id);
        return count;
    }

    synchronized void updateLastMessage(@NonNull String chatId, @NonNull ChatMessage msg) {
        lastMessageCache.put(chatId, msg);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("last_msg", msg.id);
            db.update("chats", values, "id=?", new String[]{chatId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized ChatMessage getLastMessage(@NonNull String chatId) {
        ChatMessage lastMsg = lastMessageCache.get(chatId);
        if (lastMsg != null) return lastMsg;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT last_msg FROM chats WHERE id=?", new String[]{chatId})) {
            if (!cursor.moveToNext()) return null;

            String lastMsgId = cursor.getString(0);
            if (lastMsgId == null) return null;

            ChatMessage msg = getMessage(chatId, lastMsgId);
            lastMessageCache.put(chatId, msg);
            return msg;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public Long getLastLastSeen() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT MAX(last_seen) FROM chats", null)) {
            if (!cursor.moveToNext()) return null;

            if (cursor.isNull(0)) return null;
            else return cursor.getLong(0);
        } finally {
            db.endTransaction();
        }
    }
}
