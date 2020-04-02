package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class ChatMessages extends ArrayList<PlainChatMessage> {
    public final Chat chat;

    public ChatMessages(int initialCapacity, @NonNull Chat chat) {
        super(initialCapacity);
        this.chat = chat;
    }

    public ChatMessages(@NonNull Collection<PlainChatMessage> collection, @NonNull Chat chat) {
        super(collection);
        this.chat = chat;
    }
}
