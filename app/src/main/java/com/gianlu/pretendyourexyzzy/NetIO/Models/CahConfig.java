package com.gianlu.pretendyourexyzzy.NetIO.Models;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CahConfig {
    private static final Pattern PATTERN = Pattern.compile("cah\\.(.+?)\\s=\\s(.+?);");
    public final boolean globalChatEnabled;
    public final boolean insecureIdAllowed;

    public CahConfig(String str) throws ParseException {
        Matcher matcher = PATTERN.matcher(str);

        Boolean globalChatEnabledTmp = null;
        Boolean insecureIdAllowedTmp = null;
        while (matcher.find()) {
            String name = matcher.group(1);
            String val = matcher.group(2);

            switch (name) {
                case "GLOBAL_CHAT_ENABLED":
                    globalChatEnabledTmp = Boolean.parseBoolean(val);
                    break;
                case "INSECURE_ID_ALLOWED":
                    insecureIdAllowedTmp = Boolean.parseBoolean(val);
                    break;
            }
        }

        if (globalChatEnabledTmp == null) throw new ParseException(str, 0);
        else globalChatEnabled = globalChatEnabledTmp;

        if (insecureIdAllowedTmp == null) throw new ParseException(str, 0);
        else insecureIdAllowed = insecureIdAllowedTmp;
    }
}
