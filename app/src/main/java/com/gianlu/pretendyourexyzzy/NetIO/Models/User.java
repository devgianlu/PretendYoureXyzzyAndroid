package com.gianlu.pretendyourexyzzy.NetIO.Models;

import android.content.Context;

import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import java.io.Serializable;

public class User implements BaseDrawerProfile, Serializable {
    public final String nickname;

    public User(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String getProfileName(Context context) {
        return nickname;
    }

    @Override
    public String getSecondaryText(Context context) {
        return null;
    }

    @Override
    public String getInitials(Context context) {
        return nickname.substring(0, 2);
    }
}
