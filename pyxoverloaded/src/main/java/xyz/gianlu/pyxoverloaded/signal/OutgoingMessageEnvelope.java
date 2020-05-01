package xyz.gianlu.pyxoverloaded.signal;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.protocol.CiphertextMessage;

public class OutgoingMessageEnvelope {
    private final CiphertextMessage message;
    private final int deviceId;

    public OutgoingMessageEnvelope(@NonNull CiphertextMessage message, int deviceId) {
        this.message = message;
        this.deviceId = deviceId;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("encrypted", Base64.encodeToString(message.serialize(), Base64.NO_WRAP));
        obj.put("type", message.getType());
        obj.put("destDeviceId", deviceId);
        return obj;
    }
}
