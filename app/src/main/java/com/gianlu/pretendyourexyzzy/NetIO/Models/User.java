package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;

public class User implements BaseDrawerProfile, Serializable {
    public final String nickname;
    public final String sessionId;
    public final String idCode;
    public final Sigil sigil;
    public final String persistentId;

    public User(@NonNull String sessionId, JSONObject obj) throws JSONException {
        this.sessionId = sessionId;
        this.nickname = obj.getString("n");
        this.idCode = obj.getString("idc");
        this.persistentId = obj.getString("pid");

        try {
            this.sigil = Sigil.parse(obj.getString("?"));
        } catch (ParseException ex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) throw new JSONException(ex);
            else throw new JSONException(ex.getMessage());
        }
    }

    @NonNull
    @Override
    public String getProfileName(Context context) {
        return nickname;
    }

    @NonNull
    @Override
    public String getSecondaryText(Context context) {
        return idCode;
    }

    @NonNull
    @Override
    public String getInitials(Context context) {
        return nickname.substring(0, 2);
    }

    public enum Sigil {
        ADMIN("@"), ID_CODE("+"), NORMAL_USER("");
        private final String val;

        Sigil(String val) {
            this.val = val;
        }

        @NonNull
        public static Sigil parse(@NonNull String val) throws ParseException {
            for (Sigil sigil : values())
                if (sigil.val.equals(val))
                    return sigil;

            throw new ParseException(val, 0);
        }
    }
}
