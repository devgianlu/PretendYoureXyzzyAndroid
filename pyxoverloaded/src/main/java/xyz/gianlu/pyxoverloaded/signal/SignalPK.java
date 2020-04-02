package xyz.gianlu.pyxoverloaded.signal;

import com.gianlu.commonutils.preferences.Prefs;

abstract class SignalPK {
    static final Prefs.Key SIGNAL_IDENTITY_KEY_PUBLIC = new Prefs.Key("signal_identityKeyPublic");
    static final Prefs.Key SIGNAL_IDENTITY_KEY_PRIVATE = new Prefs.Key("signal_identityKeyPrivate");
    static final Prefs.Key SIGNAL_LOCAL_REGISTRATION_ID = new Prefs.Key("signal_registrationId");
    static final Prefs.Key SIGNAL_LAST_PRE_KEY_ID = new Prefs.Key("signal_lastPreKeyId");
    static final Prefs.Key SIGNAL_DEVICE_ID = new Prefs.Key("signal_deviceId");
}
