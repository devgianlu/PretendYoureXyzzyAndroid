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
    private final Map<String, Chat> chats = new HashMap<>(64);

    ChatDatabaseHelper(Context context) {
        super(context, "chat.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats(id TEXT UNIQUE, oneParticipant TEXT, otherParticipant TEXT, last_seen LONG)");
        db.execSQL("CREATE TABLE messages(id TEXT UNIQUE, chat_id TEXT, text TEXT, timestamp LONG, `from` TEXT)");
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

    synchronized void updateLastSeen(@NotNull String chatId, long lastSeen) {
        Chat chat = chats.get(chatId);
        if (chat != null) {
            if (chat.lastSeen == lastSeen) return;
            else chat.lastSeen = lastSeen;
        }

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
    synchronized Chat getChat(@NonNull String chatId) {
        Chat chat = chats.get(chatId);
        if (chat != null) return chat;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, "id=?", new String[]{chatId}, null, null, null, null)) {
            if (cursor == null || !cursor.moveToNext()) return null;

            chat = new Chat(cursor);
            chats.put(chatId, chat);
            return chat;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Cursor getMessages(@NonNull String chatId, int limit, int offset) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.query("messages", null, "chat_id=?", new String[]{chatId}, null, null, "timestamp DESC", String.valueOf(offset + limit));
            if (cursor == null || (offset != 0 && cursor.moveToFirst() && !cursor.move(offset)))
                return null;
            return cursor;
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

    synchronized void putChats(@NonNull List<Chat> list) {
        if (list.isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Chat chat : list) {
                if (chat.participants.size() != 2)
                    throw new IllegalStateException();

                if (chats.containsKey(chat.id)) {
                    chats.put(chat.id, chat);
                } else {

                }


                ContentValues values = new ContentValues();
                values.put("id", chat.id);
                values.put("last_seen", chat.lastSeen);
                values.put("oneParticipant", chat.participants.get(0));
                values.put("otherParticipant", chat.participants.get(1));
                db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    synchronized void putChat(@NonNull Chat chat) {
        if (chat.participants.size() != 2)
            throw new IllegalStateException();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!chats.containsKey(chat.id)) chats.put(chat.id, chat);
            ContentValues values = new ContentValues();
            values.put("id", chat.id);
            values.put("last_seen", chat.lastSeen);
            values.put("oneParticipant", chat.participants.get(0));
            values.put("otherParticipant", chat.participants.get(1));
            db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    synchronized Chat findChatWith(@NotNull String username) {
        for (Chat chat : chats.values())
            if (chat.participants.contains(username))
                return chat;

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, "oneParticipant=? OR otherParticipant=?", new String[]{username, username}, null, null, null, "1")) {
            if (cursor == null || !cursor.moveToNext())
                return null;

            Chat chat = new Chat(cursor);
            chats.put(chat.id, chat);
            return chat;
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    synchronized List<Chat> getChats() {
        if (!chats.isEmpty()) return new ArrayList<>(chats.values());

        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, null, null, null, null, null, null)) {
            if (cursor == null || !cursor.moveToNext())
                return Collections.emptyList();

            List<Chat> list = new ArrayList<>(32);
            do {
                Chat chat = new Chat(cursor);
                if (!chats.containsKey(chat.id)) chats.put(chat.id, chat);
                list.add(chat);
            } while (cursor.moveToNext());

            return list;
        } finally {
            db.endTransaction();
        }
    }

    int countSinceLastSeen(@NonNull String chatId, long lastSeen) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM messages WHERE timestamp >= ? AND chat_id=?", new String[]{String.valueOf(lastSeen), chatId})) {
            if (!cursor.moveToNext()) return 0;
            else return cursor.getInt(0);
        } finally {
            db.endTransaction();
        }
    }
}
