package com.gianlu.pretendyourexyzzy.NetIO.Models;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CahConfig {
    private static final Pattern PATTERN = Pattern.compile("cah\\.(.+?)\\s=\\s(.+?);");
    private final Map<String, String> map = new HashMap<>();

    public CahConfig(String str) {
        Matcher matcher = PATTERN.matcher(str);

        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
    }

    private boolean getOrDefault(String key, boolean def) {
        String val = map.get(key);
        return val == null ? def : Boolean.parseBoolean(val);
    }

    public boolean gameChatEnabled() {
        return getOrDefault("GAME_CHAT_ENABLED", false);
    }

    public boolean globalChatEnabled() {
        return getOrDefault("GLOBAL_CHAT_ENABLED", false);
    }

    public boolean insecureIdAllowed() {
        return getOrDefault("INSECURE_ID_ALLOWED", false);
    }
}
