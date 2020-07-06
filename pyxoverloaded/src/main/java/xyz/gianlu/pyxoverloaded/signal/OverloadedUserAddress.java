package xyz.gianlu.pyxoverloaded.signal;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.whispersystems.libsignal.SignalProtocolAddress;

public class OverloadedUserAddress {
    public final String uid;
    public final int deviceId;

    public OverloadedUserAddress(@NotNull String uid, int deviceId) {
        this.uid = uid;
        this.deviceId = deviceId;
    }

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

    @NotNull
    @Override
    public String toString() {
        return "OverloadedUserAddress{" + "uid='" + uid + '\'' + ", deviceId=" + deviceId + '}';
    }
}
