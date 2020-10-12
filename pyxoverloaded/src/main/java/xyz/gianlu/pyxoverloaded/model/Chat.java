package xyz.gianlu.pyxoverloaded.model;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.adapters.Filterable;

import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;

public class Chat implements Filterable<Void> {
    public final int id;
    public final String recipient;
    public final String address;

    public Chat(@NonNull Cursor cursor) {
        id = cursor.getInt(cursor.getColumnIndex("id"));
        address = cursor.getString(cursor.getColumnIndex("address"));
        recipient = cursor.getString(cursor.getColumnIndex("recipient"));
    }

    public Chat(int id, @NonNull String recipient, @NonNull String address) {
        this.id = id;
        this.recipient = recipient;
        this.address = address;
    }

    @NonNull
    public OverloadedUserAddress deviceAddress(int deviceId) {
        return new OverloadedUserAddress(address, deviceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id == chat.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    @Nullable
    public Void[] getMatchingFilters() {
        return null;
    }
}
