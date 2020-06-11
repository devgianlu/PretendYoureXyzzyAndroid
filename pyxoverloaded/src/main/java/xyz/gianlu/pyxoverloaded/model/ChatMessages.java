package xyz.gianlu.pyxoverloaded.model;

import androidx.annotation.NonNull;

import java.util.LinkedList;

public class ChatMessages extends LinkedList<PlainChatMessage> {
    public final Chat chat;

    public ChatMessages(@NonNull Chat chat) {
        this.chat = chat;
    }
}
