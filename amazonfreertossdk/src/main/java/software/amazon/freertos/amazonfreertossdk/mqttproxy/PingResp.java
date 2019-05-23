package software.amazon.freertos.amazonfreertossdk.mqttproxy;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

import static software.amazon.freertos.amazonfreertossdk.AmazonFreeRTOSConstants.MQTT_MSG_PINGRESP;

public class PingResp {
    private static final String TAG = "PingResp";

    private static final String TYPE_KEY = "w";

    public byte[] encode() {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                    .addMap()
                    .put(TYPE_KEY, MQTT_MSG_PINGRESP)
                    .end()
                    .build());
            bytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return bytes;
    }
}
