package xyz.gianlu.pyxoverloaded;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import xyz.gianlu.pyxoverloaded.model.Chat;
import xyz.gianlu.pyxoverloaded.model.ChatMessage;
import xyz.gianlu.pyxoverloaded.model.ChatMessages;

class ChatDatabaseHelper extends SQLiteOpenHelper {
    private final LruCache<String, Chat> chatsCache = new LruCache<>(64);

    ChatDatabaseHelper(Context context) {
        super(context, "chat.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats(id TEXT UNIQUE, participants TEXT, last_seen LONG)");
        db.execSQL("CREATE TABLE messages(id TEXT UNIQUE, chat_id TEXT, text TEXT, timestamp LONG, `from` TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Nullable
    public Chat getChat(@NonNull String chatId) {
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

    @Nullable
    public Cursor getMessages(@NonNull String chatId, int limit) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try {
            return db.query("messages", null, "chat_id=?", new String[]{chatId}, null, null, "timestamp DESC", String.valueOf(limit));
        } finally {
            db.endTransaction();
        }
    }

    public void addMessage(@NonNull String chatId, @NonNull ChatMessage msg) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("id", msg.id);
            values.put("chat_id", chatId);
            values.put("text", msg.text);
            values.put("timestamp", msg.timestamp);
            values.put("from", msg.from);

            db.insert("messages", null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void putMessages(@NonNull ChatMessages list) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String chatId = list.chat.id;
            for (ChatMessage msg : list) {
                ContentValues values = new ContentValues();
                values.put("id", msg.id);
                values.put("chat_id", chatId);
                values.put("text", msg.text);
                values.put("timestamp", msg.timestamp);
                values.put("`from`", msg.from);
                db.insertWithOnConflict("messages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void putChats(@NonNull List<Chat> chats) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Chat chat : chats) {
                chatsCache.put(chat.id, chat);
                ContentValues values = new ContentValues();
                values.put("id", chat.id);
                values.put("last_seen", chat.lastSeen);
                values.put("participants", CommonUtils.join(chat.participants, ","));
                db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void putChat(@NonNull Chat chat) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            chatsCache.put(chat.id, chat);
            ContentValues values = new ContentValues();
            values.put("id", chat.id);
            values.put("last_seen", chat.lastSeen);
            values.put("participants", CommonUtils.join(chat.participants, ","));
            db.insertWithOnConflict("chats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public Chat findChatWith(@NotNull String username) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("chats", null, "participants LIKE ? OR participants LIKE ?", new String[]{username + ",%", "%," + username}, null, null, null, "1")) {
            if (cursor == null || !cursor.moveToNext())
                return null;

            return new Chat(cursor);
        } finally {
            db.endTransaction();
        }
    }
}
