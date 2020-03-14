package xyz.gianlu.pyxoverloaded;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;

final class Utils {

    private Utils() {
    }

    @NonNull
    static HttpUrl overloadedServerUrl(@NonNull String path) {
        return HttpUrl.get("http://192.168.1.25:8080/" + path); // FIXME: Testing url
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
