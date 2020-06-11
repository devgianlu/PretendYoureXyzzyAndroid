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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.ChatMessages;
import xyz.gianlu.pyxoverloaded.model.PlainChatMessage;

public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private final Map<Integer, Chat> chatsCache = new HashMap<>(64);
    private final Map<Integer, Long> lastSeenCache = new HashMap<>(64);

    ChatDatabaseHelper(Context context) {
        super(context, "chat.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats(id INTEGER PRIMARY KEY UNIQUE NOT NULL, address TEXT NOT NULL, recipient TEXT NOT NULL, last_seen LONG DEFAULT NULL)");
        db.execSQL("CREATE TABLE messages(chat_id INTEGER NOT NULL, text TEXT NOT NULL, timestamp LONG NOT NULL, `from` TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Nullable
    public PlainChatMessage getLastMessage(int chatId) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT rowid, * FROM messages WHERE chat_id=? ORDER BY timestamp DESC LIMIT 1", new String[]{String.valueOf(chatId)})) {
            if (cursor == null || !cursor.moveToNext()) return null;
            return new PlainChatMessage(chatId, cursor);
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized ChatMessages getMessagesPaginate(int chatId, long startFrom) {
        Chat chat = getChat(chatId);
        if (chat == null) return null;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT rowid, * FROM messages WHERE chat_id=? AND timestamp < ? ORDER BY timestamp DESC LIMIT 128", new String[]{String.valueOf(chatId), String.valueOf(startFrom)})) {
            if (cursor == null || !cursor.moveToFirst())
                return null;

            ChatMessages list = new ChatMessages(chat);
            do {
                list.add(new PlainChatMessage(chatId, cursor));
            } while (cursor.moveToNext());
            return list;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized ChatMessages getMessages(int chatId) {
        Chat chat = getChat(chatId);
        if (chat == null) return null;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT rowid, * FROM messages WHERE chat_id=? ORDER BY timestamp DESC LIMIT 128", new String[]{String.valueOf(chatId)})) {
            if (cursor == null || !cursor.moveToFirst())
                return null;

            ChatMessages list = new ChatMessages(chat);
            do {
                list.add(new PlainChatMessage(chatId, cursor));
            } while (cursor.moveToNext());
            return list;
        } finally {
            db.endTransaction();
        }
    }

    @NotNull
    public PlainChatMessage putMessage(@NonNull String text, long timestamp, @NotNull String fromAddress, @NonNull String fromUsername) {
        Chat chat = findChatWith(fromUsername);
        if (chat == null) chat = putChat(fromAddress, fromUsername);

        return putMessage(chat.id, text, timestamp, fromUsername);
    }

    @NonNull
    public synchronized PlainChatMessage putMessage(int chatId, @NonNull String text, long timestamp, @NonNull String fromUsername) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("chat_id", chatId);
            values.put("text", text);
            values.put("timestamp", timestamp);
            values.put("`from`", fromUsername);
            long rowId = db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful();
            return new PlainChatMessage(chatId, rowId, text, timestamp, fromUsername);
        } finally {
            db.endTransaction();
        }
    }

    synchronized void removeChat(@NonNull Chat chat) {
        chatsCache.remove(chat.id);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("chats", "id=?", new String[]{String.valueOf(chat.id)});
            db.delete("messages", "chat_id=?", new String[]{String.valueOf(chat.id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Chat getChat(int chatId) {
        Chat chat = chatsCache.get(chatId);
        if (chat != null) return chat;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM chats WHERE id=?", new String[]{String.valueOf(chatId)})) {
            if (cursor == null || !cursor.moveToNext()) return null;

            chat = new Chat(cursor);
            chatsCache.put(chatId, chat);
            return chat;
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    synchronized Chat putChat(@NonNull String address, @NonNull String recipient) {
        Chat chat = findChatWith(recipient);
        if (chat != null) return chat;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("address", address);
            values.put("recipient", recipient);
            int id = (int) db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            db.setTransactionSuccessful();

            if (id == -1) throw new IllegalStateException();

            chat = new Chat(id, recipient, address);
            chatsCache.put(id, chat);
            return chat;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Chat findChatWith(@NotNull String username) {
        for (Chat chat : chatsCache.values())
            if (chat.recipient.equals(username))
                return chat;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT * FROM chats WHERE recipient=? LIMIT 1", new String[]{username})) {
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
        try (Cursor cursor = db.rawQuery("SELECT * FROM chats", null)) {
            if (cursor == null || !cursor.moveToNext())
                return new ArrayList<>(0);

            List<Chat> list = new LinkedList<>();
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

    synchronized void updateLastSeen(int chatId, long lastSeen) {
        lastSeenCache.put(chatId, lastSeen);

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("last_seen", lastSeen);
            db.update("chats", values, "id=?", new String[]{String.valueOf(chatId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    private synchronized Long getLastSeen(int chatId) {
        Long lastSeen = lastSeenCache.get(chatId);
        if (lastSeen != null) return lastSeen;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT last_seen FROM chats WHERE id=?", new String[]{String.valueOf(chatId)})) {
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

    synchronized int countSinceLastSeen(int chatId) {
        Long lastSeen = getLastSeen(chatId);
        if (lastSeen == null) return 0;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE timestamp >= ? AND chat_id=?", new String[]{String.valueOf(lastSeen), String.valueOf(chatId)})) {
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
}
