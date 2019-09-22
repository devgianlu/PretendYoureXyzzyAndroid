package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.drawer.BaseDrawerProfile;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;

public class UserInfo implements BaseDrawerProfile, Serializable {
    public final String nickname;
    public final String idCode;
    public final Name.Sigil sigil;

    UserInfo(JSONObject obj) throws JSONException {
        this.nickname = obj.getString("n");
        this.idCode = obj.getString("idc");

        try {
            this.sigil = Name.Sigil.parse(obj.getString("?"));
        } catch (ParseException ex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) throw new JSONException(ex);
            else throw new JSONException(ex.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPrimaryText(@NonNull Context context) {
        return nickname;
    }

    @NonNull
    @Override
    public String getSecondaryText(@NonNull Context context) {
        try {
            return Pyx.Server.lastServer().name;
        } catch (Pyx.NoServersException ex) {
            Logging.log(ex);
            return idCode;
        }
    }
}
