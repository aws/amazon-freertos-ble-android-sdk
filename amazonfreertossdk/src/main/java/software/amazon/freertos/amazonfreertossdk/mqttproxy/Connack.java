/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.freertos.amazonfreertossdk.mqttproxy;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

/**
 * This class represents the MQTT CONNACK message.
 */
public class Connack {
    private static final String TAG = "MqttConnack";

    private static final String TYPE_KEY = "w";
    private static final String STATUS_KEY = "s";
    /**
     * MQTT message type.
     */
    public int type;
    /**
     * The MQTT connection status defined in {@code MqttConnectionState} enum.
     */
    public int status;

    public byte[] encode() {
        byte[] connackBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                .put(TYPE_KEY, type)
                .put(STATUS_KEY, status)
                .end()
                .build());
            connackBytes = baos.toByteArray();
        } catch (CborException e) {
            Log.e(TAG, "Failed to encode.", e);
        }
        return connackBytes;
    }
}
