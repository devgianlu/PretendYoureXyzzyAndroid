package xyz.gianlu.pyxoverloaded;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class ChatDatabaseHelper extends SQLiteOpenHelper { // TODO: Use database for caching stuff
    ChatDatabaseHelper(Context context) {
        super(context, "chat.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats(id TEXT, participants TEXT, last_seen LONG)");
        db.execSQL("CREATE TABLE messages(id TEXT, chat_id TEXT, text TEXT, timestamp LONG, `from` TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
