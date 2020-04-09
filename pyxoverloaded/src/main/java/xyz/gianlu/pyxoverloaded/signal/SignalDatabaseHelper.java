package xyz.gianlu.pyxoverloaded.signal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public final class SignalDatabaseHelper extends SQLiteOpenHelper {
    private static SignalDatabaseHelper instance = null;

    private SignalDatabaseHelper(@NonNull Context context) {
        super(context, "signal.db", null, 1);
    }

    @NonNull
    public static SignalDatabaseHelper get() {
        if (instance == null) throw new IllegalStateException();
        return instance;
    }

    public static void init(@NonNull Context context) {
        if (instance == null) instance = new SignalDatabaseHelper(context);
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS preKeys (id INTEGER, serialized BLOB)");
        db.execSQL("CREATE TABLE IF NOT EXISTS signedPreKeys (id INTEGER, serialized BLOB)");
        db.execSQL("CREATE TABLE IF NOT EXISTS identityKeys (name TEXT, deviceId INTEGER, serialized BLOB)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sessions (name TEXT, deviceId INTEGER, serialized BLOB)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
