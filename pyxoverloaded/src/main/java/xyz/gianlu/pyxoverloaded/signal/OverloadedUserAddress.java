package xyz.gianlu.pyxoverloaded.signal;

import androidx.annotation.NonNull;

import org.whispersystems.libsignal.SignalProtocolAddress;

public class OverloadedUserAddress {
    public final String uid;
    public final int deviceId;

    public OverloadedUserAddress(@NonNull String address) {
        int index;
        if ((index = address.indexOf(':')) == -1)
            throw new IllegalArgumentException(address);

        uid = address.substring(0, index);
        deviceId = Integer.parseInt(address.substring(index + 1));
    }

    @NonNull
    public SignalProtocolAddress toSignalAddress() {
        return new SignalProtocolAddress(uid, deviceId);
    }
}
