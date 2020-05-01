package xyz.gianlu.pyxoverloaded;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import xyz.gianlu.pyxoverloaded.signal.OverloadedUserAddress;

public final class Utils {

    private Utils() {
    }

    @NonNull
    static PreKeyBundle parsePreKeyBundle(@NonNull JSONObject obj) throws JSONException, InvalidKeyException {
        OverloadedUserAddress address = new OverloadedUserAddress(obj.getString("address"));

        int registrationId = obj.getInt("registrationId");
        IdentityKey identityKey = new IdentityKey(Base64.decode(obj.getString("identityKey"), 0), 0);

        JSONObject preKeyObj = obj.getJSONObject("preKey");
        int preKeyId = preKeyObj.getInt("id");
        ECPublicKey preKey = Curve.decodePoint(Base64.decode(preKeyObj.getString("key"), 0), 0);

        JSONObject signedPreKeyObj = obj.getJSONObject("signedPreKey");
        int signedPreKeyId = signedPreKeyObj.getInt("id");
        ECPublicKey signedPreKey = Curve.decodePoint(Base64.decode(signedPreKeyObj.getString("key"), 0), 0);
        byte[] signedPreKeySignature = Base64.decode(signedPreKeyObj.getString("signature"), 0);

        return new PreKeyBundle(registrationId, address.deviceId, preKeyId, preKey, signedPreKeyId, signedPreKey, signedPreKeySignature, identityKey);
    }

    @NonNull
    static JSONObject toServerJson(@NonNull SignedPreKeyRecord key) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", key.getId());
        obj.put("key", Base64.encodeToString(key.getKeyPair().getPublicKey().serialize(), Base64.NO_WRAP));
        obj.put("signature", Base64.encodeToString(key.getSignature(), Base64.NO_WRAP));
        return obj;
    }

    @NonNull
    static JSONObject toServerJson(@NonNull PreKeyRecord key) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", key.getId());
        obj.put("key", Base64.encodeToString(key.getKeyPair().getPublicKey().serialize(), Base64.NO_WRAP));
        return obj;
    }

    @NonNull
    static HttpUrl overloadedServerUrl(@NonNull String path) {
        return HttpUrl.get("http://192.168.1.25:8080/" + path); // FIXME
    }

    @NonNull
    static RequestBody singletonJsonBody(@NonNull String key, String value) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(key, value);
        return jsonBody(obj);
    }

    @NonNull
    static RequestBody jsonBody(@NonNull JSONObject obj) {
        return RequestBody.create(obj.toString().getBytes(), MediaType.get("application/json"));
    }
}
